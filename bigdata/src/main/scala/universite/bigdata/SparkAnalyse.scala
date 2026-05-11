package universite.bigdata

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import scala.util.{Try, Success, Failure}

// ─────────────────────────────────────────────
// Module Big Data : SparkAnalyse
// Analyse des données historiques avec Spark
// ─────────────────────────────────────────────
object SparkAnalyse {

  // Initialisation de la session Spark (locale)
  def creerSession(): SparkSession =
    SparkSession.builder()
      .appName("GestionUniversitaire-BigData")
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "4")   // optimisé pour petits datasets locaux
      .config("spark.ui.enabled", "false")            // désactiver l'UI pour les démos console
      .getOrCreate()

  // ─────────────────────────────────────────
  // Chargement des DataFrames
  // ─────────────────────────────────────────

  def chargerEtudiants(spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/etudiants.csv")

  def chargerNotes(spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/notes.csv")

  def chargerAbsences(spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/absences.csv")

  def chargerPaiements(spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/paiements.csv")

  def chargerMatieres(spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/matieres.csv")

  // ─────────────────────────────────────────
  // Nettoyage des données
  // ─────────────────────────────────────────

  def nettoyerEtudiants(df: DataFrame): DataFrame =
    df.na.drop(Seq("matricule", "filiere", "niveau"))     // supprimer lignes sans clé
      .filter(col("matricule").isNotNull)
      .filter(col("email").contains("@"))                 // email valide
      .dropDuplicates("matricule")                        // dédoublonner

  def nettoyerNotes(df: DataFrame): DataFrame =
    df.na.drop(Seq("matricule", "matiere"))
      .filter(col("controle_continu").between(0, 20))     // notes dans [0,20]
      .filter(col("examen").between(0, 20))
      .dropDuplicates("id_note")

  def nettoyerAbsences(df: DataFrame): DataFrame =
    df.na.drop(Seq("matricule", "matiere", "heures"))
      .filter(col("heures") > 0)
      .dropDuplicates("id_absence")

  def nettoyerPaiements(df: DataFrame): DataFrame =
    df.na.drop(Seq("matricule", "montant_total", "montant_paye"))
      .filter(col("montant_total") >= 0)
      .filter(col("montant_paye") >= 0)
      .dropDuplicates("id_paiement")

  // Détecter les valeurs manquantes dans un DataFrame
  def rapportValeursManquantes(df: DataFrame, nomTable: String): Unit = {
    println(s"\n── Valeurs manquantes : $nomTable ──")
    val total = df.count()
    df.columns.foreach { col_name =>
      val manquants = df.filter(df(col_name).isNull || df(col_name) === "").count()
      if (manquants > 0)
        println(f"  $col_name%-30s : $manquants%3d / $total (${manquants * 100.0 / total}%.1f%%)")
    }
  }

  // ─────────────────────────────────────────
  // Analyses avec Spark SQL / DataFrame API
  // ─────────────────────────────────────────

  // Nombre d'étudiants par filière
  def etudiantsParFiliere(spark: SparkSession): DataFrame = {
    val df = nettoyerEtudiants(chargerEtudiants(spark))
    df.groupBy("filiere")
      .count()
      .withColumnRenamed("count", "nb_etudiants")
      .orderBy(desc("nb_etudiants"))
  }

  // Calcul des moyennes par étudiant (40% CC + 60% exam)
  def moyennesParEtudiant(spark: SparkSession): DataFrame = {
    val df = nettoyerNotes(chargerNotes(spark))
    df.withColumn("moyenne_matiere",
        col("controle_continu") * 0.4 + col("examen") * 0.6
      )
      .groupBy("matricule")
      .agg(
        avg("moyenne_matiere").alias("moyenne_generale"),
        count("id_note").alias("nb_matieres")
      )
      .orderBy(desc("moyenne_generale"))
  }

  // Taux de réussite par filière (join étudiants + notes)
  def tauxReussiteParFiliere(spark: SparkSession): DataFrame = {
    val etudiants = nettoyerEtudiants(chargerEtudiants(spark))
      .select("matricule", "filiere")

    val moyennes = moyennesParEtudiant(spark)
      .select("matricule", "moyenne_generale")

    val joint = etudiants.join(moyennes, Seq("matricule"), "left")
      .withColumn("admis", when(col("moyenne_generale") >= 10, 1).otherwise(0))

    joint.groupBy("filiere")
      .agg(
        count("matricule").alias("total"),
        sum("admis").alias("admis"),
        avg("moyenne_generale").alias("moyenne_filiere")
      )
      .withColumn("taux_reussite",
        round(col("admis") * 100.0 / col("total"), 1)
      )
      .orderBy(desc("taux_reussite"))
  }

  // Total d'absences par étudiant
  def absencesParEtudiant(spark: SparkSession): DataFrame = {
    val df = nettoyerAbsences(chargerAbsences(spark))
    df.groupBy("matricule")
      .agg(
        sum("heures").alias("total_heures"),
        count("id_absence").alias("nb_absences"),
        sum(when(col("justifiee") === "Non", col("heures")).otherwise(0))
          .alias("heures_non_justifiees")
      )
      .orderBy(desc("total_heures"))
  }

  // Synthèse financière
  def syntheseFinanciere(spark: SparkSession): DataFrame = {
    val df = nettoyerPaiements(chargerPaiements(spark))
    df.agg(
      sum("montant_total").alias("total_attendu"),
      sum("montant_paye").alias("total_encaisse"),
      sum(col("montant_total") - col("montant_paye")).alias("total_restant"),
      round(sum("montant_paye") * 100.0 / sum("montant_total"), 1).alias("taux_recouvrement")
    )
  }

  // Tendance des absences par matière
  def tendanceAbsencesParMatiere(spark: SparkSession): DataFrame = {
    val absences = nettoyerAbsences(chargerAbsences(spark))
    val matieres = chargerMatieres(spark)
      .select(col("id_matiere"), col("nom_matiere"))

    absences
      .groupBy("matiere")
      .agg(sum("heures").alias("total_heures"))
      .join(matieres, absences("matiere") === matieres("id_matiere"), "left")
      .select(
        col("nom_matiere"),
        col("total_heures")
      )
      .orderBy(desc("total_heures"))
  }

  // Performances par promotion (niveau)
  def performancesParPromotion(spark: SparkSession): DataFrame = {
    val etudiants = nettoyerEtudiants(chargerEtudiants(spark))
      .select("matricule", "niveau", "filiere")
    val moyennes  = moyennesParEtudiant(spark)

    etudiants.join(moyennes, Seq("matricule"), "left")
      .groupBy("niveau", "filiere")
      .agg(
        avg("moyenne_generale").alias("moyenne_promo"),
        count("matricule").alias("nb_etudiants")
      )
      .orderBy("niveau", desc("moyenne_promo"))
  }

  // ─────────────────────────────────────────
  // Sauvegarde des résultats
  // ─────────────────────────────────────────

  def sauvegarderCSV(df: DataFrame, chemin: String): Unit =
    Try {
      df.coalesce(1)
        .write
        .option("header", "true")
        .mode("overwrite")
        .csv(chemin)
      println(s"[OK] Sauvegardé → $chemin")
    }.recover { case ex => println(s"[ERREUR] Sauvegarde échouée : ${ex.getMessage}") }

  def sauvegarderParquet(df: DataFrame, chemin: String): Unit =
    Try {
      df.coalesce(1)
        .write
        .mode("overwrite")
        .parquet(chemin)
      println(s"[OK] Sauvegardé (Parquet) → $chemin")
    }.recover { case ex => println(s"[ERREUR] Sauvegarde échouée : ${ex.getMessage}") }

  // ─────────────────────────────────────────
  // Point d'entrée : lancer toutes les analyses
  // ─────────────────────────────────────────

  def lancerAnalyses(): Unit = {
    println("\n╔══════════════════════════════════════════╗")
    println("║       MODULE BIG DATA — SPARK SCALA      ║")
    println("╚══════════════════════════════════════════╝")

    val spark = creerSession()
    spark.sparkContext.setLogLevel("ERROR")   // masquer les logs Spark verbeux

    Try {
      // 1. Rapport des valeurs manquantes
      println("\n── Contrôle qualité des données ──")
      rapportValeursManquantes(chargerEtudiants(spark),  "etudiants")
      rapportValeursManquantes(chargerNotes(spark),      "notes")
      rapportValeursManquantes(chargerAbsences(spark),   "absences")
      rapportValeursManquantes(chargerPaiements(spark),  "paiements")

      // 2. Étudiants par filière
      println("\n── Étudiants par filière ──")
      etudiantsParFiliere(spark).show()
      sauvegarderCSV(etudiantsParFiliere(spark), "../data/output/exports/etudiants_par_filiere")

      // 3. Moyennes par étudiant
      println("\n── Top moyennes ──")
      moyennesParEtudiant(spark).show(10)
      sauvegarderCSV(moyennesParEtudiant(spark), "../data/output/exports/moyennes_etudiants")

      // 4. Taux de réussite par filière
      println("\n── Taux de réussite par filière ──")
      tauxReussiteParFiliere(spark).show()
      sauvegarderCSV(tauxReussiteParFiliere(spark), "../data/output/exports/taux_reussite")

      // 5. Absences par étudiant
      println("\n── Absences par étudiant ──")
      absencesParEtudiant(spark).show()

      // 6. Synthèse financière
      println("\n── Synthèse financière ──")
      syntheseFinanciere(spark).show()

      // 7. Tendance absences par matière
      println("\n── Tendance absences par matière ──")
      tendanceAbsencesParMatiere(spark).show()

      // 8. Performances par promotion
      println("\n── Performances par promotion ──")
      performancesParPromotion(spark).show()
      sauvegarderParquet(performancesParPromotion(spark), "../data/output/exports/performances_promo")

    } match {
      case Success(_) => println("\n[OK] Toutes les analyses Spark terminées.")
      case Failure(ex) => println(s"\n[ERREUR] Analyse Spark : ${ex.getMessage}")
    }

    spark.stop()
  }
}
