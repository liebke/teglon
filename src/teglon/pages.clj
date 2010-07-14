(ns ^{:doc "This namespace provides page templates for use by teglon.web."
       :author "David Edgar Liebke"}
    teglon.pages)


(defn index-page [] 
  (str "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
       "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
       " <head>"
       "  <title>Teglon</title>"
       " </head>"
       " <body>"
       "  <h1>Teglon</h1>"
       "  <ul>"
       "    <li><a href=\"/models\">Models</a></li>"
       "  </ul>"
       " </body>"
       "</html>"))

(defn status-404 [uri]
  (str "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
       "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
       " <head>"
       "  <title>404 - Not Found</title>"
       " </head>"
       " <body>"
       "  <h1>404 - Not Found</h1>"
       "<p>No such page: " uri "</p>"
       " </body>"
       "</html>"))