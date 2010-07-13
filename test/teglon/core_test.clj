(ns teglon.core-test
  (:use [teglon.core])
  (:use [clojure.test]))

;; using a snapshot of clojars.org for testing

(deftest test-repo-indexing
  (init-repo "/Users/liebke/Desktop/clojars/clojars.org/repo")
  (is (= 966 (count (keys @*cljr-repo-db*))))
  (is (= 347 (count (keys @*cljr-index-by-group*))))
  (is (= 485 (count (keys @*cljr-index-by-group-name*))))
  (is (= 985 (count (keys @*cljr-index-by-text*)))))

(deftest test-get-model
  (let [model (get-model "incanter" "incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model)))))
  (let [model (get-model "incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model)))))
  (let [model (get-model "incanter/incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model))))))

(deftest test-get-all-versions-of-model
  (let [model-versions #{"1.0-new-SNAPSHOT" "1.0-master-SNAPSHOT"
			 "1.2.3-SNAPSHOT" "1.2.1" "1.2.2-SNAPSHOT"
			 "1.2.1-SNAPSHOT" "1.2.2" "0.9.0"
			 "1.0-SNAPSHOT"}]
    (let [models (get-all-versions-of-model "incanter" "incanter")]
     (is (and (coll? models) (= 9 (count models))))
     (is (= model-versions (set (map :version models)))))
    (let [models (get-all-versions-of-model "incanter")]
      (is (and (coll? models) (= 9 (count models))))
      (is (= model-versions (set (map :version models)))))
    (let [models (get-all-versions-of-model "incanter/incanter")]
      (is (and (coll? models) (= 9 (count models))))
      (is (= model-versions (set (map :version models)))))))

(deftest test-search-repo
  (let [search-results (search-repo "incanter")]
    (is (= 65 (count search-results)))
    (is (= (set (map :group search-results))
	   #{"incanter" "incanter-latex"}))))

(deftest test-get-models-by-group
  (let [models (get-models-by-group "incanter")]
    (is (= 63 (count models)))
    (is (= #{"incanter"} (set (map :group models))))))

(deftest test-list-project-repo-contents
  (is (= 58 (count (list-project-repo-contents "clj-time" "0.1.0-SNAPSHOT"))))
  (is (= 9 (count (list-project-jars "clj-time" "0.1.0-SNAPSHOT"))))
  (is (= 9 (count (list-project-poms "clj-time" "0.1.0-SNAPSHOT")))))

(deftest test-add-to-maven-repo
  (let [jar-file (.getAbsolutePath (last (list-project-jars "clj-time" "0.1.0-SNAPSHOT")))
	pom-file (.getAbsolutePath (last (list-project-poms "clj-time" "0.1.0-SNAPSHOT")))]
    (repository-dir "/tmp/teglon/repo/")
    (add-to-maven-repo jar-file pom-file)
    (let [model (get-model "clj-time" "0.1.0-SNAPSHOT")
	  project-dir "clj-time/clj-time/0.1.0-SNAPSHOT/"
	  jar-file-regex (re-pattern
			  (str (repository-dir) project-dir
			       "clj-time-.*\\.jar$"))
	  pom-file-regex (re-pattern
			  (str (repository-dir) project-dir
			       "clj-time-.*\\.pom$"))]
      (is (map? model))
      (is (and (= "clj-time" (:name model))
	       (= "clj-time" (:group model))
	       (= "0.1.0-SNAPSHOT" (:version model))))
      (is (re-matches jar-file-regex
		      (.getAbsolutePath (last (list-project-jars "clj-time" "0.1.0-SNAPSHOT")))))
      (is (re-matches pom-file-regex
		      (.getAbsolutePath (last (list-project-poms "clj-time" "0.1.0-SNAPSHOT"))))))))
