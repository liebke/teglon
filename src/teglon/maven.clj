(ns ^{:doc "This namespace is based entirely on the clojars.maven namespace from the Clojars-web project."
       :author "Alex Osborne"}
    teglon.maven
  (:require [clojure.java.io :as io])
  (:use [clojure.contrib.condition :only [raise]])
  (:import [org.apache.maven.model Model
	    Dependency
	    Contributor]
           [org.apache.maven.model.io.xpp3 MavenXpp3Writer MavenXpp3Reader]
	   org.apache.maven.artifact.repository.ArtifactRepositoryFactory
	   org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout
	   org.apache.maven.artifact.factory.ArtifactFactory
	   org.apache.maven.project.artifact.ProjectArtifactMetadata
	   org.apache.maven.artifact.deployer.ArtifactDeployer
	   org.apache.maven.settings.Settings
	   org.apache.maven.settings.MavenSettingsBuilder
	   org.codehaus.plexus.embed.Embedder
	   [java.io PushbackReader
	    StringWriter
	    File
	    FileWriter]))

;; weirdo magic plexus container stuff to make maven work
;; TODO: find out if it's safe to just leave these hanging around like
;; this
(def embedder (doto (Embedder.) (.start)))
(def container (.getContainer embedder))

(defn read-vec
  "Produces a vector of clojure data structures read from file."
  [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (loop [v []]
      (let [x (read rdr false ::end)]
        (if (not= x ::end)
          (recur (conj v x))
          v)))))

(defn make-dependency
  "Constructs a Maven Dependency object.  The jarsym should be
  groupId/artifactId."
  [jarmap]
  (doto (Dependency.)
    (.setGroupId (:group jarmap))
    (.setArtifactId (:name jarmap))
    (.setVersion (:version jarmap))
    ;;(.setClassifier (:classifier jarmap))
    ))

(defn make-contributor
  [name]
  (doto (Contributor.)
    (.setName name)))

(defn make-model
  "Produces a maven Model from a defjar map."
  [dj]
  (.println System/err (str (:dependencies dj)))
  (doto (Model.)
    (.setModelVersion "4.0.0")
    (.setGroupId (:group dj))
    (.setArtifactId (:name dj))
    (.setVersion (:version dj))
    ;;(.setClassifier (:classifier dj))

    (.setDescription (:description dj))
    (.setUrl (:homepage dj))
    (.setContributors (vec (map make-contributor (:authors dj))))

    (.setPackaging "jar")
    (.setDependencies (vec (map make-dependency (:dependencies dj))))))


(defn model-to-map [model]
  {:name (.getArtifactId model)
   :group (.getGroupId model)
   :version (.getVersion model)
   :description (.getDescription model)

   :homepage (.getUrl model)
   :authors (vec (map #(.getName %) (.getContributors model)))
   :dependencies (vec (map (fn [d]
                             {:group (.getGroupId d)
                              :name (.getArtifactId d)
                              :version (.getVersion d)})
                              (.getDependencies model)))})

(defn model-to-xml
  "Converts a maven model to a string of XML."
  [model]
  (let [sw (StringWriter.)]
    (.write (MavenXpp3Writer.) sw model)
    (str sw)))

(defn model-to-file
  "Converts a maven model to a temporary file (containing XML)."
  [model]
  (let [f (File/createTempFile "mvninstall" ".pom")]
    (.deleteOnExit f)
    (with-open [writer (FileWriter. f)]
      (.write (MavenXpp3Writer.) writer model))
    f))

(defn read-pom
  "Reads a pom file returning a maven Model object."
  [file]
  (with-open [reader (io/reader file)]
    (.read (MavenXpp3Reader.) reader)))

(defn make-repo
  "Does the crazy factory voodoo necessary to get an ArtifactRepository object."
  [id url]
  (-> (.lookup container ArtifactRepositoryFactory/ROLE)
      (.createDeploymentArtifactRepository id
					   url
					   (.lookup container
						    ArtifactRepositoryLayout/ROLE
						    "default")
					   true)))

(defn make-settings
  "Makes a maven Settings object"
  []
  (.buildSettings (.lookup container MavenSettingsBuilder/ROLE)))

(defn make-local-repo
  "Returns the local maven repository."
  []
  (let [path (.getLocalRepository (make-settings))
        url (if (.startsWith path "file:") path (str "file://" path))]
    (make-repo "local" url)))

(defn make-artifact
  "Makes a maven artifact from a maven model (more crazy voodoo)."
  [m]
  (let [artifact (.createArtifactWithClassifier
                  (.lookup container ArtifactFactory/ROLE)
                  (.getGroupId m)
                  (.getArtifactId m)
                  (.getVersion m)
                  (.getPackaging m)
                  nil)]
    (.addMetadata artifact (ProjectArtifactMetadata.
                            artifact (model-to-file m)))
    artifact))

(defn deploy-model [jarfile model repo-path]
  (.deploy
   (.lookup container ArtifactDeployer/ROLE)
   jarfile (make-artifact model) (make-repo "cljr" repo-path)
   (make-local-repo)))
