(ns 
  ^{:doc "Functions for managing metadata persistence."
    :author "David Edgar Liebke"}
  teglon.data
  (:require [teglon.scheduler :as sched]
	    [clojure.java.io :as io]
	    [clojure.contrib.logging :as log]))


(defn data-file [data-name]
  (io/file "data" (str data-name ".clj")))

(defn save-data [data-name data]
  (let [file (data-file data-name)]
    (.mkdirs (.getParentFile file))
    (log/info (str "Writing " data-name " to " (.getAbsolutePath file)))
    (spit file (pr-str data))))

(defn read-data
  ([data-name data-ref]
     (dosync (ref-set data-ref (read-data data-name))))
  ([data-name]
     (let [file (data-file data-name)]
       (if (.exists file)
	 (do
	   (log/info (str "Reading " data-name " from " (.getAbsolutePath file)))
	   (read-string (slurp file)))
	 (log/info (str "File for " data-name ", " (.getAbsolutePath file) ", does not exist."))))))
