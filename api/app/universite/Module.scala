package universite

import com.google.inject.AbstractModule
import java.time.Clock

/**
 * Ce module configure les liaisons pour l'injection de dépendances (Guice).
 * Actuellement utilisé pour les configurations de base.
 */
class Module extends AbstractModule {

  override def configure(): Unit = {
    // Bindings spécifiques si nécessaire
    // bind(classOf(Clock)).toInstance(Clock.systemDefaultZone)
  }

}
