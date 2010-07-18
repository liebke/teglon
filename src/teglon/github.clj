(ns ^{:doc "Client api for github."
      :author "David Edgar Liebke"}
  teglon.github
  (:require [clojure.string :as s])
  (:require [teglon.repo :as repo]
	    [teglon.db :as db]
	    [teglon.web.client :as client]))


(def *github-api-base-url* "http://github.com/api/v2/json")
(def *github-api-repos-url* (str *github-api-base-url* "/repos"))
(def *github-api-search-url* (str *github-api-repos-url* "/search"))

(defn list-branches [user-name repo-name]
  (let [url (str *github-api-repos-url* "/show/" user-name "/" repo-name "/branches")]
    (client/json-request url)))

(defn get-sha [user-name repo-name branch-name]
  (let [branch (keyword branch-name)]
    (-> (list-branches user-name repo-name)
       :branches
       branch)))

(defn search [query]
  (let [url (str *github-api-search-url* "/" query)]
    (client/json-request url)))

(defn get-local-head-sha
  ([] (get-local-head-sha ".git"))
  ([project-dir]
     (let [dot-git-dir (str project-dir (repo/sep) ".git" (repo/sep))
	   head-ref (slurp (str dot-git-dir "HEAD"))
	   head-location (apply hash-map
				(map s/trim
				     (s/split head-ref
					      #":")))]
       (s/trim-newline (slurp (str dot-git-dir (head-location "ref")))))))

(defn get-remote-sha
  ([origin branch-name] (get-remote-sha ".git" origin branch-name))
  ([project-dir origin branch-name]
     (let [dot-git-dir (str ".git" (repo/sep))
	   remote-sha (slurp (str dot-git-dir
				  "refs" (repo/sep)
				  "remotes" (repo/sep)
				  origin (repo/sep) branch-name))]
       (s/trim-newline remote-sha))))

