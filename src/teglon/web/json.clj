(ns ^{:doc "This is the core of the RESTful api."
      :author "David Edgar Liebke"}
  teglon.web.json
  (:require [teglon.db :as db]
	    [clojure.string :as s])
  (:use compojure.core
	[clojure.contrib.json :only (json-str)]))


(def *base-url* "/api/v1/json")
(def *model-show-url* (str *base-url* "/model/show"))

(defn model-to-uri
  ([model]
     (model-to-uri *model-show-url* model))
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

(defn model-show [artifact-id]
  (json-str (apply db/get-model
		   (parse-group-name-version artifact-id))))

(defn group-show [group]
  (json-str (map add-uri-to-model
		 (db/list-models-by-group group))))

(defn versions-show [group-name]
  (json-str (map add-uri-to-model
		 (apply db/list-all-versions-of-model
		    (parse-group-name group-name)))))

(defn models-children [artifact-id]
  (json-str (map add-uri-to-model
		 (apply db/list-models-that-depend-on
		    (parse-group-name-version artifact-id)))))

(defn versions-children [group-name]
  (json-str (map add-uri-to-model
		 (apply db/list-models-that-depend-on
		    (parse-group-name group-name)))))

(defn group-children [group]
  (json-str (map add-uri-to-model
		 (db/list-models-that-depend-on group))))

(defn models-show []
  (json-str (map add-uri-to-model
		 (db/list-all-models))))

(defn models-search [query]
  (json-str (map add-uri-to-model
		 (db/search-repo query))))
