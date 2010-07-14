(ns ^{:doc "This namespace provides a RESTful API built on the Aleph web server for managing, browsing, and querying Maven repositories."
       :author "David Edgar Liebke"}
    teglon.web
  (:use [teglon.core :as repo]
	[aleph]
	[clojure.contrib.json :only (pprint-json json-str)]
	[clojure.java.io :as io])
  (:require [clojure.string :as s]))


(def *server* (ref nil))

(defn uri-to-map [uri]
  (let [[path & query] (s/split uri #"\?")
	path-seq (next (s/split path #"/"))
	query-seq (when (seq query)
		    (s/split (first query) #"&"))]
    {:uri uri
     :path-seq path-seq
     :query-seq query-seq}))

(defn error-page-handler [request uri-map]
  (println (str "File not found 404: " (:uri request)))
  (respond! request
	   {:status 404
	    :body (str "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
		       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
		       "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
		       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
		       " <head>"
		       "  <title>404 - Not Found</title>"
		       " </head>"
		       " <body>"
		       "  <h1>404 - Not Found</h1>"
		       " </body>"
		       "</html>")}))

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
	        (json-str (get-all-models))
	      (= 1 (count args))
	        (json-str (apply repo/get-all-versions-of-model args))
	      (= 2 (count args))
	        (json-str (apply repo/get-all-versions-of-model args))
	      (= 3 (count args))
	        (json-str (apply repo/get-model args)))]
    (respond! request
	      {:status 200
	       :header {"Content-Type" "text/json"}
	       :body resp})))

(defn search-handler [request uri-map]
  (let [resp (json-str (repo/search-repo (first (:query-seq uri-map))))]
    (respond! request
	      {:status 200
	       :body resp})))

(def route-map {"models" models-handler
		"search" search-handler
		"repo" file-handler})

(defn repo-handler [request]
  (let [uri-map (uri-to-map (:uri request))
	handler (route-map (first (:path-seq uri-map)))]
    (println (str "Request: " (:uri request)))
    (if handler
      (handler request uri-map)
      (error-page-handler request uri-map))))

(defn start-server
  ([] (start-server nil))
  ([repo-dir & [port]]
     (let [port (or port 8080)]
       (println "Initializing repository server...")
       (if repo-dir
	 (repo/init-repo repo-dir)
	 (repo/init-repo))
       (println (str "Starting webserver on port " port "..."))
       (dosync (ref-set *server* (run-aleph repo-handler {:port port})))
       (println "Web server started."))))

(defn stop-server []
  (.close @*server*))


;;;;;;;;;;;;;;;;;;;;;;;;;; 

;; (def server (start-server))
;; (.close server)

;; http://localhost:8080/models/incanter/incanter/1.2.3-SNAPSHOT
;; http://localhost:8080/models/incanter/incanter
;; http://localhost:8080/models/incanter
;; http://localhost:8080/models
;; http://localhost:8080/search?haml
