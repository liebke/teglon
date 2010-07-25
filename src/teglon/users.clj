(ns 
  ^{:doc "Functions for managing users."
    :author "David Edgar Liebke"}
  teglon.users
  (:require [teglon.data :as data]
	    [teglon.security :as sec]
	    [teglon.scheduler :as sched]))

(def *users* (ref {}))
(def *user-roles* (ref {}))

(def *valid-user-roles* #{:admin :member})

(defn save-users-periodically []
  (let [five-min (* 1000 60 5)]
    (sched/schedule-periodic #(data/save-data "users" @*users*)
			     five-min five-min)))

(defn load-users []
  (dosync (ref-set *users* (or (data/read-data "users") {}))))

(defn init-users []
  (load-users)
  (save-users-periodically))

(defn add-user [username password]
  (let [token (sec/get-sha1-string password)
	user-map {:username username
		  :token token}]
    (if (get @*users* username)
      {:status :error
       :message (str "The username " username " already exists.")}
      (do
	(dosync
	 (alter *users* assoc username user-map))
	{:status :success
	 :message (str "User " username " created.")}))))

(defn get-user [username]
  (get @*users* username))

(defn authenticate-user [username password]
  (let [user (get-user username)
	token (:token user)]
    (when user
      (= token (sec/get-sha1-string password)))))

(defn remove-user [username]
  (if (get-user username)
    (do
      (dosync (alter *users* dissoc username))
      {:status :succuss
       :message (str "User " username " has been removed.")})
    {:status :error
     :message (str "User " username " does not exist.")}))

(defn add-role [username role]
  (if (*valid-user-roles* role)
    (do
      (dosync
       (let [user (get-user username)
	     roles (or (:roles user) #{})
	     updated-user (assoc user :roles (conj roles role))]
	 (alter *users* assoc username updated-user)))
      {:status :success
       :message (str "User role, " role ", successfully added to user " username)})
    {:status :error
     :message (str "Tried to add invalid user role, " role ", to user " username)}))

(defn has-role? [username role]
  (let [user-roles (:roles (get-user username))]
    (get user-roles role)))

(defn remove-role [username role]
  (if (*valid-user-roles* role)
    (do
      (dosync
       (let [user (get-user username)
	     roles (or (:roles user) #{})
	     updated-user (assoc user :roles (clojure.set/difference roles #{role}))]
	 (alter *users* assoc username updated-user)))
      {:status :success
       :message (str "User role, " role ", successfully removed from user " username)})
    {:status :error
     :message (str "Tried to remove an invalid user role, " role ", from user " username)}))