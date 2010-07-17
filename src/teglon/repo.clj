(ns ^{:doc "This namespace provides functions for managing, browsing,
 and querying Maven repositories."
       :author "David Edgar Liebke"}
    teglon.repo
    (:require [teglon.maven :as maven]
	      [teglon.db :as db]
	      [clojure.java.io :as io]
	      [clojure.string :as s]))

(def *repository-dir* (ref nil))

(defn sep [] (java.io.File/separator))

(defn repository-dir
  ([] @*repository-dir*)
  ([directory-path]
     (dosync (ref-set *repository-dir* directory-path))))

(defn add-to-maven-repo [jar-file pom-file]
  (let [jar-file (io/file jar-file)
	pom-file (io/file pom-file)
        model (maven/read-pom pom-file)
	repo-url (str "file://" (repository-dir))]
    (maven/deploy-model jar-file model repo-url)
    (db/add-model-map-to-db (maven/model-to-map model))))

(defn- list-sub-dirs [directory-name]
  "Not currently used."
  (let [dir (io/file directory-name)]
    (filter #(.isDirectory %) (.listFiles dir))))

(defn- directory-tree [root-dir-name]
  "Not currently used."
  {(.getName (io/file root-dir-name))
   (apply merge (for [dir (list-sub-dirs root-dir-name)]
		  (directory-tree (.getAbsolutePath dir))))})

(defn- find-files-ending-with [root-dir-name suffix]
  (loop [[file & more-files :as files] (seq (.listFiles (io/file root-dir-name)))
	 results []]
    (if (seq files)
      (if (.isDirectory file)
	(recur (concat (seq (.listFiles file)) more-files) results)
	(recur more-files
	       (if (.endsWith (.getName file) suffix)
		 (conj results file)
		 results)))
      results)))

(defn list-poms-in-repo [root-dir-name]
  (find-files-ending-with root-dir-name ".pom"))

(defn list-jars-in-repo [root-dir-name]
  (find-files-ending-with root-dir-name ".jar"))

(defn list-pom-base-names-in-repo [root-dir-name]
  (map #(s/replace (.getAbsolutePath %) #".pom$" "")
       (list-poms-in-repo root-dir-name)))

(defn get-project-repo-dir
  ([artifact-id version]
     (let [[group name] (db/artifact-id-to-group-name artifact-id)]
       (get-project-repo-dir group name version)))
  ([group name version]
     (let [group-dirs (s/replace group #"\." (sep))
	   name-dirs (s/replace name #"\." (sep))
	   dir-name (str (repository-dir) (sep)
			 group-dirs (sep)
			 name-dirs (sep)
			 version)]
       (io/file dir-name))))

(defn list-project-repo-contents
  ([artifact-id version]
     (let [[group name] (db/artifact-id-to-group-name artifact-id)]
       (list-project-repo-contents group name version)))
  ([group name version]
     (sort-by #(.lastModified %)
	      (seq (.listFiles (get-project-repo-dir group name version))))))

(defn list-project-jars
  ([artifact-id version]
     (let [[group name] (db/artifact-id-to-group-name artifact-id)]
       (list-project-jars group name version)))
  ([group name version]
     (filter #(.endsWith (.getName %) ".jar")
	     (list-project-repo-contents group name version))))

(defn list-project-poms
  ([artifact-id version]
     (let [[group name] (db/artifact-id-to-group-name artifact-id)]
       (list-project-poms group name version)))
  ([group name version]
     (filter #(.endsWith (.getName %) ".pom")
	     (list-project-repo-contents group name version))))

(defn index-repo
  ([] (index-repo (repository-dir)))
  ([repo-dir]
     (doall
      (doseq [pom-file (list-poms-in-repo repo-dir)]
	(-> pom-file maven/read-pom maven/model-to-map db/add-model-map-to-db)))))

(defn default-repo-dir []
  (str (System/getProperty "user.home")
		     (sep) ".m2"
		     (sep) "repository"))

(defn init-repo
  ([]
     (init-repo (default-repo-dir)))
  ([repo-dir]
     (println "Initializing repository database...")
     (println (str "Setting repository directory to " repo-dir))
     (repository-dir repo-dir)
     (db/init-db)
     (println "Indexing Maven repository metadata...")
     (index-repo repo-dir)
     (println "Indexing complete.")))
