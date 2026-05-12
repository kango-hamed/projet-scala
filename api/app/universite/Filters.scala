package universite

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import universite.filters.CorsFilter

class Filters @Inject()(corsFilter: CorsFilter)
  extends DefaultHttpFilters(corsFilter)
