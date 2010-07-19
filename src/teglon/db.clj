(ns ^{:doc "This namespace provides functions for managing the in-memory
 database for Maven metadata."
      :author "David Edgar Liebke"}
  teglon.db
  (:require [teglon.maven :as maven]
	    [clojure.java.io :as io]
	    [clojure.string :as s]))

(defn init-db []
  (def *teglon-repo-db* (ref {}))
  (def *teglon-index-by-group* (ref {}))
  (def *teglon-index-by-group-name* (ref {}))
  (def *teglon-index-by-text* (ref {}))
  (def *teglon-index-by-dependency* (ref {}))
  (def *teglon-index-by-dependency-group* (ref {}))
  (def *teglon-index-by-dependency-group-name* (ref {})))

(defn db-initialized? []
  (let [db @*teglon-repo-db*]
    (and (not (nil? db)) (not (empty? db)))))

(defn model-key
  ([model-map]
     (let [{:keys [group name version]} model-map]
       {:group group :name name :version version}))
  ([group name version]
     {:group group :name name :version version}))

(defn group-name-key
  ([model-map]
     (let [{:keys [group name]} model-map]
       {:group group :name name}))
  ([group name]
     {:group group :name name}))

(defmulti group-key map?)

(defmethod group-key true
  ([model-map]
     {:group (:group model-map)}))

(defmethod group-key false
  ([group]
     {:group group}))

(defn artifact-id-to-group-name
  [artifact-id]
  (if (.contains artifact-id "/")
    (s/split artifact-id #"/")
    [artifact-id artifact-id]))

(defn- update-db
  ([db-map model-map]
     (merge db-map {(model-key model-map) model-map})))

(defn- update-group-name-index
  ([index-map model-map]
     (let [group-name-key (group-name-key model-map)
	   current-group-name-val (or (get @*teglon-index-by-group-name* group-name-key)
				      #{})
	   group-name-val (conj current-group-name-val (model-key model-map))]
       (assoc index-map group-name-key group-name-val))))

(defn- update-group-index
  ([index-map model-map]
     (let [group-key (group-key model-map)
	   current-group-val (or (get @*teglon-index-by-group* group-key) #{})
	   group-val (conj current-group-val (model-key model-map))]
       (assoc index-map group-key group-val))))

(defn- update-text-index
  ([index-map model-map]
     (let [mdl-key (model-key model-map)
	   text-key (.toLowerCase
		     (apply str
		      (interpose " "
				 [(:name model-map)
				  (:group model-map)
				  (:description model-map)
				  (:homepage model-map)
				  (apply str (interpose " " (:authors model-map)))])))]
       (assoc index-map text-key mdl-key))))

(defn- update-dependency-index
  ([model-map]
     (let [updater (fn [dep-index-map dep-model-map]
		     (let [dep-group-name-key (model-key dep-model-map)
			   current-val (or (get @*teglon-index-by-dependency* dep-group-name-key)
					   #{})
			   dep-group-name-val (conj current-val (model-key model-map))]
		       (assoc dep-index-map dep-group-name-key dep-group-name-val)))]
       (doseq [d (:dependencies model-map) :when d]
	 (dosync (alter *teglon-index-by-dependency* updater d))))))

(defn- update-dependency-group-index
  ([model-map]
     (let [updater (fn [dep-index-map dep-model-map]
		     (let [dep-group-key (group-key dep-model-map)
			   current-val (or (get @*teglon-index-by-dependency-group* dep-group-key)
					   #{})
			   dep-group-val (conj current-val (model-key model-map))]
		       (assoc dep-index-map dep-group-key dep-group-val)))]
       (doseq [d (:dependencies model-map) :when d]
	 (dosync (alter *teglon-index-by-dependency-group* updater d))))))

(defn- update-dependency-group-name-index
  ([model-map]
     (let [updater (fn [dep-index-map dep-model-map]
		     (let [dep-group-name-key (group-name-key dep-model-map)
			   current-val (or (get @*teglon-index-by-dependency-group-name* dep-group-name-key)
					   #{})
			   dep-group-name-val (conj current-val (model-key model-map))]
		       (assoc dep-index-map dep-group-name-key dep-group-name-val)))]
       (doseq [d (:dependencies model-map) :when d]
	 (dosync (alter *teglon-index-by-dependency-group-name* updater d))))))

(defn clear-db []
  (do
    (dosync
     (ref-set *teglon-repo-db* {})
     (ref-set *teglon-index-by-group* {})
     (ref-set *teglon-index-by-group-name* {})
     (ref-set *teglon-index-by-text* {}))))

(defn update-dependency-indices [model-map]
  (update-dependency-index model-map)
  (update-dependency-group-index model-map)
  (update-dependency-group-name-index model-map))

(defn update-primary-indices [model-map]
  (dosync (alter *teglon-index-by-group* update-group-index model-map)
	  (alter *teglon-index-by-group-name* update-group-name-index model-map)
	  (alter *teglon-index-by-text* update-text-index model-map)))

(defn add-model-map-to-db [model-map]
  (dosync (alter *teglon-repo-db* update-db model-map))
  (update-primary-indices model-map)
  (update-dependency-indices model-map))

(defn search-repo [text]
  (let [text-keys (filter #(.contains % text) (keys @*teglon-index-by-text*))
	model-keys (map #(get @*teglon-index-by-text* %) text-keys)]
    model-keys))

(defn list-all-models [] (keys @*teglon-repo-db*))

(defn get-all-models [] (vals @*teglon-repo-db*))

(defn get-model
  ([artifact-id version]
     (let [[group name] (artifact-id-to-group-name artifact-id)]
       (get-model group name version)))
  ([group name version]
     (get @*teglon-repo-db* (model-key group name version))))

(defn list-all-versions-of-model
  ([artifact-id]
     (let [[group name] (artifact-id-to-group-name artifact-id)]
       (list-all-versions-of-model group name)))
  ([group name]
     (get @*teglon-index-by-group-name* (group-name-key group name))))

(defn get-all-versions-of-model
  ([artifact-id]
     (let [[group name] (artifact-id-to-group-name artifact-id)]
       (get-all-versions-of-model group name)))
  ([group name]
    (let [model-keys (get @*teglon-index-by-group-name* (group-name-key group name))]
      (map #(get @*teglon-repo-db* %) model-keys))))

(defn list-models-by-group [group]
  (get @*teglon-index-by-group* {:group group}))

(defn get-models-by-group [group]
  (let [model-keys (get @*teglon-index-by-group* (group-key group))]
    (map #(get @*teglon-repo-db* %) model-keys)))

(defn list-models-that-depend-on
  ([group name version]
     (get @*teglon-index-by-dependency* (model-key group name version)))
  ([group name]
     (get @*teglon-index-by-dependency-group-name* (group-name-key group name)))
  ([group]
     (get @*teglon-index-by-dependency-group* (group-key group))))

(def search-score
     (memoize
      (fn [model]
	(count (get @*teglon-index-by-dependency-group-name*
		    (select-keys model [:group :name]))))))

(defn add-score-to-model [model]
  (assoc model :score (search-score model)))

(defn sort-models [models]
  (sort-by :score > (map add-score-to-model models)))

