(ns ^{:doc "Web interface for teglon.web"
      :author "David Edgar Liebke"}
  teglon.web.html
  (:require [teglon.db :as db]
	    [teglon.repo :as repo]
	    [teglon.web.util :as util]
	    [clojure.java.io :as io])
  (:use compojure.core
	[hiccup core form-helpers]
	[hiccup.page-helpers :only (include-css)]
	[clojure.contrib.json :only (json-str)]))


(def *base-url* "/html")
(def *show-model-html-url* (str *base-url* "/model/show"))
(def *show-group-html-url* (str *base-url* "/group/show"))
(def *show-group-name-html-url* (str *base-url* "/versions/show"))
(def stylesheet "/static/teglon.css")

(defn model-to-uri
  ([model]
     (let [{:keys [group name version]} model]
       (model-to-uri group name version)))
  ([group name version]
     (str *show-model-html-url* "/" group "/" name "/" version)))

(defn group-to-uri
  ([group]
     (group-to-uri *show-group-html-url* group))
  ([base-url group]
     (str base-url "/" group)))

(defn group-name-to-uri
  ([model]
     (group-name-to-uri (:group model) (:name model)))
  ([group name]
     (group-name-to-uri *show-group-name-html-url* group name))
  ([base-url group name]
     (str base-url "/" group "/" name)))

(defn add-uri-to-model
  ([model]
     (assoc model :uri (model-to-uri model)))
  ([uri-fn model]
     (assoc model :uri (uri-fn model))))

(defn main-search-form
  ([] (main-search-form ""))
  ([query]
     (html
      [:p 
       (form-to [:get "/html/models/search"]
		(text-field {:value query :size "85"} :q)
		(submit-button "Search"))])))

(defn search-form
  ([] (search-form ""))
  ([query]
     (html
      (form-to [:get "/html/models/search"]
	       (text-field {:value query :size "75"} :q)
	       (submit-button "Search")))))

(defn main-masthead []
  [:div {:class "main-masthead"}
   [:a {:href "/"}
    [:img {:src "http://incanter.org/images/teglon/teglon.png"
	   :alt "Teglon"
	   :class "main-masthead-logo"}]]
   (main-search-form)
   [:ul {:class "main-masthead-links"}
    [:li [:a {:href "/html/models/show"} "Browse Libraries"]]
    [:li [:a {:href "/upload"} "Upload Library"]]]])

(defn masthead
  ([] (masthead ""))
  ([query]
     [:div {:class "masthead"}
      [:a {:href "/"}
       [:img {:src "http://incanter.org/images/teglon/teglon.png"
	      :alt "Teglon"
	      :class "masthead-logo"}]]
      (search-form)]))

(defn index-page []
  (html [:head
	 [:title "Teglon"]]
	(include-css stylesheet)
	[:body
	 (main-masthead)]))

(defn upload-file-form []
  [:div {:class "upload-form"}
   [:form {:action "/upload"
	   :method "POST"
	   :enctype "multipart/form-data"}
    [:table
     [:tr
      [:td (label "pom-file" [:strong "pom file: "])]
      [:td (file-upload "pom-file")]]
     [:tr
      [:td (label "jar-file" [:strong "jar file: "])]
      [:td (file-upload "jar-file")]]
     [:tr
      [:td]
      [:td (submit-button "Upload files")]]]]])

(defn upload-page
  ([] (html [:head
	     [:title "Teglon"]]
	    (include-css stylesheet)
	    [:body
	     (masthead)
	     (upload-file-form)]))
  ([model pom-filename jar-filename]
     (let [{:keys [group name version]} model]
       (html [:head
	     [:title "Teglon"]]
	    (include-css stylesheet)
	    [:body
	     (masthead)
	     (upload-file-form)
	     [:div {:class "upload-msg"}
	      [:strong "Success: "]
	      [:a {:href (model-to-uri model)} group " / " name " / " version]
	      " added to Teglon."]]))))

(defn list-children [group name]
  (let [children (into #{} (map #(select-keys % [:group :name :uri])
				(db/list-models-that-depend-on group name)))]
    (html
     (if (seq children)
       [:ul
	(for [child children]
	  (let [{:keys [group name version]} child]
	    [:li [:a {:href (group-name-to-uri child)} (str group "/" name)]]))]
       " N/A"))))

(defn list-dependencies [model]
  (let [deps (:dependencies model)]
    (html
     (if (seq deps)
       [:ul
	(for [dep deps]
	  (let [{:keys [group name version]} dep
		in-repo? (db/get-model group name version)]
	    [:li (if in-repo?
		   [:a {:href (model-to-uri dep)}
		    (str group "/" name "/" version)]
		   (str group "/" name "/" version))]))]
       " N/A"))))

(defn list-pom-files [model project-dir]
  (let [{:keys [group name version]} model]
    (html
    [:ul
     (for [f (reverse (repo/list-project-poms group name version))]
       (let [file-name (.getName f)]
	 [:li [:a {:href (str project-dir "/" file-name)} file-name]]))])))

(defn list-jar-files [model project-dir]
  (let [{:keys [group name version]} model]
    [:ul
     (for [f (reverse (repo/list-project-jars group name version))]
       (let [file-name (.getName f)]
	 [:li [:a {:href (str project-dir "/" file-name)} file-name]]))]))

(defn model-show
  ([artifact-id]
     (let [[group name version] (util/parse-group-name-version artifact-id)]
       (model-show group name version)))
  ([group name version]
     (let [model (db/get-model group name version)
	   {:keys [group name version description homepage authors]} model
	   artifact-id (str group "/" name "/" version)
	   project-dir (str "/repo/"
			    (repo/get-project-repo-relative-dir group
								name
								version))]
       (html [:head
	      [:title (str "Teglon: " artifact-id)]
	      (include-css stylesheet)]
	     [:body
	      (masthead)
	      [:h1 [:a {:href "/html/models/show"} "repo"] " / " [:a {:href (group-to-uri group)} group] " / "
	       [:a {:href (group-name-to-uri group name)} name] " / " version]
	      [:div {:class "library-details"}
	       [:h3 "Library Details"]
	       [:ul
		[:li [:strong "Description: "]
		 (if description description "N/A")]
		[:li [:strong "Last Updated: "]
		 (or (repo/project-last-updated model) "N/A")]
		[:li [:strong "Authors: "]
		 (if (seq authors)
		   (apply str (interpose ", " authors))
		   "N/A")]
		[:li [:strong "Homepage: "]
		 (if homepage
		   [:a {:href homepage} homepage]
		   "N/A")]
		[:li [:strong "Dependencies:"] (list-dependencies model)]
		[:li [:strong "Projects in repo that use this library:"]
		 (list-children group name)]]]
	      [:div {:class "repo-contents"}
	       [:h3 [:a {:href (str "/repo/" (repo/get-project-repo-relative-dir group name version) "/")}
		     "Repository Contents"]]
	       [:ul
		[:li [:strong "Jar file(s)"] (list-jar-files model project-dir)]
		[:li [:strong "Pom file(s)"] (list-pom-files model project-dir)]]]]))))

(defn models-search [query]
  (let [results (db/sort-models
		 (into #{} (map #(select-keys % [:group :name :uri])
				(map (partial add-uri-to-model group-name-to-uri)
				     (db/search-repo query)))))]
    (html [:head
	   [:title "Teglon Search Results"]
	   (include-css stylesheet)]
	  [:body
	   (masthead query)
	   (if-not (seq results)
	     [:h2 "No results"]
	     [:ul
	     (for [result results]
	       (let [{:keys [group name version uri description score]} result]
		 [:li [:a {:href uri}
		       (str group "/" name "/" version)]
		  [:span {:id "score"} "   (score " score ")"]]))])])))

(defn- list-versions [group name versions]
  (html
   [:h1 [:a {:href "/html/models/show"} "repo"] " / " [:a {:href (group-to-uri group)} group] " / " name]
   [:div {:class "versions"}
    [:h3 "Versions"]
    [:ul
     (for [version versions :when version]
       (let [{:keys [group name version uri]} version]
	 [:li [:a {:href uri} version]]))]]))

(defn versions-show
  ([group-name]
     (let [[group name] (util/parse-group-name group-name)]
       (versions-show group name)))
  ([group name]
     (let [versions (map add-uri-to-model
			 (db/list-all-versions-of-model group name))]
       (if (= 1 (count versions))
	 {:status 302 :headers {"Location" (model-to-uri group name (:version (first versions)))}}
	 (html [:head
		[:title (str "Teglon: " group "/" name)]
		(include-css stylesheet)]
	       [:body
		(masthead)
		(if (seq versions)
		  (list-versions group name versions)
		  [:h2 "Artifacts not found"])])))))

(defn- list-artifacts [group artifacts]
  (let [artifacts (into #{} (map #(select-keys % [:group :name :uri]) artifacts))]
    (html
     [:h1 [:a {:href "/html/models/show"} "repo"] " / " group]
     [:div {:class "libraries"}
      [:h3 "Libraries"]
      [:ul
       (for [artifact artifacts]
	 (let [{:keys [group name version uri]} artifact]
	   [:li [:a {:href uri} name]]))]])))

(defn group-show [group]
  (let [artifacts (map (partial add-uri-to-model group-name-to-uri)
		       (db/list-models-by-group group))]
    (cond
     (= 1 (count (into #{} (map :name artifacts))))
       {:status 302 :headers {"Location" (group-name-to-uri group (:name (first artifacts)))}}
      :else
        (html [:head
	       [:title (str "Teglon: " group)]
	       (include-css stylesheet)]
	      [:body
	       (masthead)
	       (if (seq artifacts)
		 (list-artifacts group artifacts)
		 [:h2 "Group not found"])]))))

(defn- list-groups [groups]
  (html
   [:h1 "repo / "]
   [:div {:class "groups"}
    [:h3 "Groups"]
    [:ul
     (for [group groups :when group]
       [:li [:a {:href (group-to-uri group)} group]])]]))

(defn groups-show []
  (let [groups (sort (map :group (keys @db/*teglon-index-by-group*)))]
    (html [:head
	   [:title (str "Teglon: Groups")]
	   (include-css stylesheet)]
	  [:body
	   (masthead)
	   (if (seq groups)
	     (list-groups groups)
	     [:h2 "No groups found"])])))

(defn repo-directory-listing
  ([]
     (repo-directory-listing nil))
  ([relative-path]
     (let [directory-path (str (repo/repository-dir) "/" relative-path)
	   directory-file (io/file directory-path)
	   parent-dir (let [p (when relative-path (.getParent (io/file relative-path)))]
			(when p (str p "/")))]
       (html [:head
	      [:title "Teglon: " relative-path]
	      (include-css stylesheet)]
	     [:body
	      (masthead)
	      [:h1 {:class "dir-list"}
	       (if relative-path "/repo/" "/repo") relative-path]
	      [:table {:class "dir-list-table"}
	       [:tr [:th "Name"] [:th "Last Modified"]]
	       (when relative-path
		 [:tr [:td [:a {:href (str "/repo/" parent-dir)} (str ".. /")]]])
	       (for [f (.listFiles directory-file) :when f]
		 (let [f-name (.getName f)
		       last-modified (.lastModified f)]
		   (if (.isDirectory f)
		     [:tr [:td [:a {:href (str f-name "/")} (str f-name " /")]]]
		     [:tr [:td [:a {:href f-name} f-name] " "]
		          [:td [:span {:class "dir-list-date"} (java.util.Date. last-modified)]]])))]]))))

(defn missing-file [request]
  (html
   [:head
    [:title "Teglon: No Such File"]
    (include-css stylesheet)]
   [:body
    (masthead)
    [:h2 "Status 404"]
    [:strong "The file you are looking for cannot be found: "] (request :uri)]))

