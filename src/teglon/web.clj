(ns ^{:doc "This namespace provides Teglon's RESTful API built on the Aleph web
 server for managing, browsing, and querying Maven repositories."
      :author "David Edgar Liebke"}
  teglon.web
  (:require [teglon.repo :as repo]
	    [teglon.db :as db]
	    [teglon.pages :as pages]
	    [compojure.route :as route]
	    [clojure.string :as s])
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

(def *json-base-url* "/api/v1/json")
(def *json-model-show-url* (str *json-base-url* "/model/show"))

(defn model-to-uri
  ([model]
     (model-to-uri *json-model-show-url* model))
  ([base-url model]
     (let [{:keys [group name version]} model]
       (str base-url "/" group "/" name "/" version))))

(defn add-uri-to-model
  ([model]
     (assoc model :uri (model-to-uri model)))
  ([base-url model]
     (assoc model :uri (model-to-uri base-url model))))

(defn parse-group-name-version [group-name-version]
  (let [gnv-seq (s/split group-name-version #"/")
	group (apply str (interpose "." (drop-last 2 gnv-seq)))
	name (first (take-last 2 gnv-seq))
	version (last gnv-seq)]
    [group name version]))

(defn parse-group-name [group-name]
  (let [gn-seq (s/split group-name #"/")
	group (apply str (interpose "." (drop-last gn-seq)))
	name (last gn-seq)]
    [group name]))

(defn json-model-show [artifact-id]
  (json-str (apply db/get-model
		   (parse-group-name-version artifact-id))))

(defn json-group-show [group]
  (json-str (map add-uri-to-model
		 (db/list-models-by-group group))))

(defn json-versions-show [group-name]
  (json-str (map add-uri-to-model
		 (apply db/list-all-versions-of-model
		    (parse-group-name group-name)))))

(defn json-models-children [artifact-id]
  (json-str (map add-uri-to-model
		 (apply db/list-models-that-depend-on
		    (parse-group-name-version artifact-id)))))

(defn json-versions-children [group-name]
  (json-str (map add-uri-to-model
		 (apply db/list-models-that-depend-on
		    (parse-group-name group-name)))))

(defn json-group-children [group]
  (json-str (map add-uri-to-model
		 (db/list-models-that-depend-on group))))

(defn json-models-show []
  (json-str (map add-uri-to-model
		 (db/list-all-models))))

(defn json-models-search [query]
  (json-str (map add-uri-to-model
		 (db/search-repo query))))

(defn teglon-app [repo-dir]
  (routes
   (GET *json-model-show-url* [& artifact-id]
	(json-model-show (artifact-id "*")))
   (GET (str *json-base-url* "/versions/show/*") [& group-name]
	(json-versions-show (group-name "*")))
   (GET (str *json-base-url* "/group/show/*") [& group]
	(json-group-show (group "*")))
   (GET (str *json-base-url* "/models/show") []
	(json-models-show))
   (GET (str *json-base-url* "/models/search") [q]
	(json-models-search q))
   (GET (str *json-base-url* "/children/model/show/*") [& artifact-id]
	(json-models-children (artifact-id "*")))
   (GET (str *json-base-url* "/children/versions/show/*") [& group-name]
	(json-versions-children (group-name "*")))
   (GET (str *json-base-url* "/children/group/show/*") [& group]
	(json-group-children (group "*")))
   (GET "/repo*/" request "<h1>Directory without an index file</h1>")
   (route/files "/repo" {:root repo-dir})
   (route/not-found "<h1>Move along</h1>")
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


