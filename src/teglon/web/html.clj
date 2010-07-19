(ns ^{:doc "Web interface for teglon.web"
      :author "David Edgar Liebke"}
  teglon.web.html
  (:require [teglon.db :as db]
	    [teglon.web.util :as util])
  (:use compojure.core
	[hiccup core form-helpers]
	[clojure.contrib.json :only (json-str)]))


(def *base-url* "/html")
(def *show-model-html-url* (str *base-url* "/model/show"))
(def *show-group-html-url* (str *base-url* "/group/show"))
(def *show-group-name-html-url* (str *base-url* "/versions/show"))

(defn model-to-uri
  ([model]
     (model-to-uri *show-model-html-url* model))
  ([base-url model]
     (let [{:keys [group name version]} model]
       (str base-url "/" group "/" name "/" version))))

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

(defn masthead []
  [:a {:href "/"}
   [:img {:src "http://incanter.org/images/teglon/teglon.png"
	  :height "100"
	  :alt "Teglon"}]])

(defn search-form
  ([] (search-form ""))
  ([query]
     (html
      [:p (form-to [:get "/html/models/search"]
		   (text-field {:value query :size "65"} :q)
		   (submit-button "Search"))])))

(defn index-page []
  (html [:head
	 [:title "Teglon"]]
	[:body
	 (masthead)
	 (search-form)
	 [:a {:href "/html/models/show"} "Browse groups"]]))

(defn list-children [group name]
  (let [children (into #{} (map #(select-keys % [:group :name :uri])
				(db/list-models-that-depend-on group name)))]
    (html
     [:ul
      (for [child children]
	(let [{:keys [group name version]} child]
	  [:li [:a {:href (group-name-to-uri child)} (str group "/" name)]]))])))

(defn list-dependencies [model]
  (let [deps (:dependencies model)]
    (html
     [:ul
      (for [dep deps]
	(let [{:keys [group name version]} dep
	      in-repo? (db/get-model group name version)]
	  [:li (if in-repo?
		 [:a {:href (model-to-uri dep)}
		  (str group "/" name "/" version)]
		 (str group "/" name "/" version))]))])))

(defn model-show [artifact-id]
  (let [model (apply db/get-model
		     (util/parse-group-name-version artifact-id))
	{:keys [group name version description homepage authors]} model
	artifact-id (str group "/" name "/" version)]
    (html [:head
	   [:title (str "Teglon: " artifact-id)]]
	  [:body
	   (masthead)
	   (search-form)
	   [:h2 [:a {:href (group-to-uri group)} group] " / "
	    [:a {:href (group-name-to-uri group name)} name] " / " version]
	   [:ul
	    [:li [:strong "Description: "]
	     description]
	    [:li [:strong "Authors: "]
	     (apply str (interpose ", " authors))]
	    [:li [:strong "Homepage: "]
	     (when homepage [:a {:href homepage} homepage])]
	    [:li [:strong "Dependencies:"]
	     (list-dependencies model)]
	    [:li [:strong "Projects in repo that use this library:"]
	     (list-children group name)]]])))

(defn models-search [query]
  (let [results (db/sort-models
		 (into #{} (map #(select-keys % [:group :name :uri])
				(map (partial add-uri-to-model group-name-to-uri)
				     (db/search-repo query)))))]
    (html [:head
	   [:title "Teglon Search Results"]]
	  [:body
	   (masthead)
	   (search-form query)
	   (if-not (seq results)
	     [:h2 "No results"]
	     [:ul
	     (for [result results]
	       (let [{:keys [group name version uri description score]} result]
		 [:li [:a {:href uri}
		       (str group "/" name "/" version)]
		  [:span {:id "score"} "   (score " score ")"]]))])])))

(defn- list-artifacts [group artifacts]
  (let [artifacts (into #{} (map #(select-keys % [:group :name :uri]) artifacts))]
    (html
     [:h2 group]
     [:strong "Libraries:"]
     [:ul
      (for [artifact artifacts]
	(let [{:keys [group name version uri]} artifact]
	  [:li [:a {:href uri} name]]))])))

(defn group-show [group]
  (let [artifacts (map (partial add-uri-to-model group-name-to-uri)
		       (db/list-models-by-group group))]
    (html [:head
	   [:title (str "Teglon: " group)]]
	  [:body
	   (masthead)
	   (search-form)
	   (if (seq artifacts)
	     (list-artifacts group artifacts)
	     [:h2 "Group not found"])])))

(defn- list-versions [group name versions]
  (html
   [:h2 [:a {:href (group-to-uri group)} group] " / " name]
   [:strong "Versions:"]
   [:ul
    (for [version versions]
      (let [{:keys [group name version uri]} version]
	[:li [:a {:href uri} version]]))]))

(defn versions-show [group-name]
  (let [[group name] (util/parse-group-name group-name)
	versions (map add-uri-to-model
		      (db/list-all-versions-of-model group name))]
    (html [:head
	   [:title (str "Teglon: " group "/" name)]]
	  [:body
	   (masthead)
	   (search-form)
	   (if (seq versions)
	     (list-versions group name versions)
	     [:h2 "Artifacts not found"])])))


(defn- list-groups [groups]
  (html
   [:strong "Groups:"]
   [:ul
    (for [group groups]
      [:li [:a {:href (group-to-uri group)} group]])]))

(defn groups-show []
  (let [groups (sort (map :group (keys @db/*teglon-index-by-group*)))]
    (html [:head
	   [:title (str "Teglon: Groups")]]
	  [:body
	   (masthead)
	   (search-form)
	   (if (seq groups)
	     (list-groups groups)
	     [:h2 "No groups found"])])))

