(ns ^{:doc "This namespace provides Teglon's RESTful API built on the Aleph web
 server for managing, browsing, and querying Maven repositories."
      :author "David Edgar Liebke"}
  teglon.web
  (:require [teglon.repo :as repo]
	    [teglon.web.json :as json]
	    [teglon.web.html :as html]
	    [compojure.route :as route])
  (:use compojure.core
	[ring.adapter.jetty :only (run-jetty)]
	[ring.middleware.file :only (wrap-file)]
	[ring.middleware.file-info :only (wrap-file-info)]))


(defn teglon-app [repo-dir]
  (routes
   (GET (str json/*model-show-url* "/*") [& artifact-id]
	(json/model-show (artifact-id "*")))
   (GET (str json/*base-url* "/versions/show/*") [& group-name]
	(json/versions-show (group-name "*")))
   (GET (str json/*base-url* "/group/show/*") [& group]
	(json/group-show (group "*")))
   (GET (str json/*base-url* "/models/show") []
	(json/models-show))
   (GET (str json/*base-url* "/models/search") [q]
	(json/models-search q))
   (GET (str json/*base-url* "/children/model/show/*") [& artifact-id]
	(json/models-children (artifact-id "*")))
   (GET (str json/*base-url* "/children/versions/show/*") [& group-name]
	(json/versions-children (group-name "*")))
   (GET (str json/*base-url* "/children/group/show/*") [& group]
	(json/group-children (group "*")))
   (GET (str html/*base-url* "/model/show/*") [& artifact-id]
	(html/model-show (artifact-id "*")))
   (GET (str html/*base-url* "/models/search") [q]
	(html/models-search q))
   (GET (str html/*base-url* "/group/show/*") [& group]
	(html/group-show (group "*")))
   (GET (str html/*base-url* "/versions/show/*") [& group-name]
	(html/versions-show (group-name "*")))
   (GET (str html/*base-url* "/models/show") []
	(html/groups-show))
   (GET "/repo" request {:status 302 :headers {"Location" "/repo/"}})
   (GET "/repo/" []
	(html/repo-directory-listing))
   (GET "/repo/*/" [& relative-path]
	(html/repo-directory-listing (str (relative-path "*") "/")))
   (GET "/" request (html/index-page))
   (route/files "/repo" {:root repo-dir})
   (route/files "/static" {:root "public"})
   (ANY "*" request {:status 404 :body (html/missing-file request)})
   (GET "/echo" request (prn-str request))))

(def *server* (ref nil))

(defn start-server
  ([] (start-server (repo/default-repo-dir)))
  ([repo-dir & [port]]
     (let [port (or port 8080)]
       (println "Initializing Teglon server...")
       (repo/init-repo repo-dir)
       (println (str "Starting webserver on port " port "..."))
       (dosync (ref-set *server* (run-jetty (teglon-app repo-dir)
					    {:port port
					     :join? false})))
       (println "Web server started."))))

(defn stop-server []
  (println "Stopping Teglon server...")
  (.stop @*server*)
  (println "Server stopped."))


