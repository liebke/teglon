(ns ^{:doc "This namespace provides Teglon's RESTful API built on the Aleph web
 server for managing, browsing, and querying Maven repositories."
      :author "David Edgar Liebke"}
  teglon.web
  (:require [teglon.repo :as repo]
	    [teglon.db :as db]
	    [teglon.pages :as pages])
  (:use compojure.core
	[clojure.contrib.json :only (pprint-json json-str)]
	[ring.adapter.jetty :only (run-jetty)]
	[ring.middleware.file :only (wrap-file)]
	[ring.middleware.file-info :only (wrap-file-info)]))


;; (use 'teglon.web)
;; (start-server "/Users/liebke/Desktop/clojars/clojars.org/repo")
;; (stop-server)

;; (start-server "/tmp/teglon/repo")
;; (stop-server)
;; http://localhost:8080/api/v1/json/tree/show/incanter/incanter-core/1.2.3-SNAPSHOT
;; http://localhost:8080/api/v1/json/tree/show/incanter/incanter-core
;; http://localhost:8080/api/v1/json/tree/show/incanter
;; http://localhost:8080/api/v1/json/tree/show
;; http://localhost:8080/api/v1/json/tree/search?q=incanter

(defroutes main-routes
  (GET "/api/v1/json/tree/show/:group/:name/*" [group name & version]
       (json-str (db/get-model group name (version "*"))))
  (GET "/api/v1/json/tree/show/:group/:name" [group name]
       (json-str (db/list-all-versions-of-model group name)))
  (GET "/api/v1/json/tree/show/:group" [group]
       (json-str (db/list-models-by-group group)))
  (GET "/api/v1/json/tree/show" []
       (json-str (db/list-all-models)))
  (GET "/api/v1/json/tree/search" [q]
       (json-str (db/search-repo q)))
  (GET "/echo" request (prn-str request))
  (ANY "*" []
       {:status 404
	:headers {"Content-Type" "text/html"}
	:body "<h1>Page not found</h1>"}))

(def *server* (ref nil))

(defn init-web-server
  ([]
     (init-web-server (repo/default-repo-dir)))
  ([repo-dir]
     (wrap! main-routes
	    (:file repo-dir)
	    (:file-info))))

(defn start-server
  ([] (start-server (repo/default-repo-dir)))
  ([repo-dir & [port]]
     (let [port (or port 8080)]
       (println "Initializing Teglon server...")
       (repo/init-repo repo-dir)
       (init-web-server repo-dir)
       (println (str "Starting webserver on port " port "..."))
       (dosync (ref-set *server* (run-jetty #'main-routes
					    {:port port
					     :join? false})))
       (println "Web server started."))))

(defn stop-server []
  (println "Stopping Teglon server...")
  (.stop @*server*)
  (println "Server stopped."))


