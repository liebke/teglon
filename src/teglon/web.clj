(ns ^{:doc "This namespace provides Teglon's RESTful API built on the Aleph web
 server for managing, browsing, and querying Maven repositories."
       :author "David Edgar Liebke"}
    teglon.web
    (:use [teglon.repo :as repo]
	  [teglon.db :as db]
	  [teglon.pages :as pages]
	  [aleph]
	  [clojure.contrib.json :only (pprint-json json-str)]
	  [clojure.java.io :as io])
    (:require [clojure.string :as s]))


(def *server* (ref nil))

(defn uri-to-map [uri]
  (let [[path & query] (s/split uri #"\?")
	path-seq (next (s/split path #"/"))
	query-map (when query
		    (println "Query: " query)
		    (into {} (map #(s/split % #"=") (-> query first (s/split #"&")))))
	uri-map {:uri uri
		 :path-seq path-seq
		 :query-map query-map}]
    (println (str "URI Map: " uri-map))
    uri-map))

(defn index-handler [request uri-map]
  (respond! request
	    {:status 200
	     :header {"Content-Type" "text/html"}
	     :body (pages/index-page)}))

(defn error-page-handler [request uri-map]
  (println (str "File not found 404: " (:uri request)))
  (respond! request
	   {:status 404
	    :body (pages/status-404 (:uri request))}))

(defn directory-handler [request uri-map file]
  (let [dir-contents (json-str (map #(.getName %)
				    (seq (.listFiles file))))]
    (respond! request {:status 200
		       :header {"Content-Type" "text/json"}
		       :body dir-contents})))
  
(defn file-handler
  [request uri-map]
  (let [args (next (:path-seq uri-map))
	file (io/file (str "/Users/liebke/Desktop/clojars/clojars.org"
			   (:uri uri-map)))]
    (if (.exists file)
      (if (.isDirectory file)
	(directory-handler request uri-map file)
	(respond! request {:status 200
			   :header {"Content-Type" "application/xml"}
			   :body file}))
      (error-page-handler request uri-map))))

(defn models-handler [request uri-map]
  (let [args (next (:path-seq uri-map))
	resp (cond
	      (= 0 (count args))
	        (json-str (db/list-all-models))
	      (= 1 (count args))
	        (json-str (apply db/list-models-by-group args))
	      (= 2 (count args))
	        (json-str (apply db/list-all-versions-of-model args))
	      (= 3 (count args))
	        (json-str (apply db/get-model args)))]
    (respond! request
	      {:status 200
	       :header {"Content-Type" "text/json"}
	       :body resp})))

(defn search-handler [request uri-map]
  (let [resp (json-str (db/search-repo (get (:query-map uri-map) "q")))]
    (respond! request
	      {:status 200
	       :body resp})))

(def route-map {"models" models-handler
		"search" search-handler
		"repo" file-handler})

(defn repo-handler [request]
  (let [uri (:uri request)
	uri-map (uri-to-map uri)
	handler (route-map (first (:path-seq uri-map)))]
    (println (str "Request: " uri))
    (cond
     (= "/" uri)
       (index-handler request uri-map)
     handler
       (handler request uri-map)
     :else
       (error-page-handler request uri-map))))

(defn start-server
  ([] (start-server nil))
  ([repo-dir & [port]]
     (let [port (or port 8080)]
       (println "Initializing Teglon server...")
       (if repo-dir
	 (repo/init-repo repo-dir)
	 (repo/init-repo))
       (println (str "Starting webserver on port " port "..."))
       (dosync (ref-set *server* (run-aleph repo-handler {:port port})))
       (println "Web server started."))))

(defn stop-server []
  (println "Stopping Teglon server...")
  (.close @*server*)
  (println "Server stopped."))


;;;;;;;;;;;;;;;;;;;;;;;;;; 

;; (def server (start-server))
;; (.close server)

;; http://localhost:8080/models/incanter/incanter/1.2.3-SNAPSHOT
;; http://localhost:8080/models/incanter/incanter
;; http://localhost:8080/models/incanter
;; http://localhost:8080/models
;; http://localhost:8080/search?haml
