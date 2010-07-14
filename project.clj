(defproject teglon "1.0.0-SNAPSHOT"
  :description "Teglon provides Clojure and RESTful APIs for managing, browsing, and searching Maven repositories."
  :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
		 
		 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [org.apache.maven/maven-artifact-manager "2.2.1"]
                 [org.apache.maven/maven-model "2.2.1"]
                 [org.apache.maven/maven-project "2.2.1"]
                 [org.apache.maven.wagon/wagon-file "1.0-beta-6"]

		 [aleph "0.1.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :main teglon.main)
