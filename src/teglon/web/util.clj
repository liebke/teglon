(ns ^{:doc "Utility functions for teglon.web"
      :author "David Edgar Liebke"}
  teglon.web.util
  (:require [clojure.string :as s]))

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


