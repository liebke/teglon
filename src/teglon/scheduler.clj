(ns 
  ^{:doc "Scheduling tools for periodic tasks"
    :author "Stuart Sierra"}
  teglon.scheduler
  (:require [clojure.contrib.logging :as log])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)))

(def ^{:private true
       :doc "The number of threads in the ScheduledThreadPoolExecutor.
             Should reflect the number of necessary scheduled tasks"}
     num-threads 3)

(def ^{:private true} pool (atom nil))

(defn- thread-pool []
  {:pre [(or (instance? ScheduledThreadPoolExecutor @pool)
             (nil? @pool))]
   :post [(instance? ScheduledThreadPoolExecutor @pool)
          (not (.isShutdown @pool))]}
  (swap! pool (fn [p] (or p (ScheduledThreadPoolExecutor. num-threads))))) 

(defn protect-from-exceptions
  "Given a function of no arguments, returns a function that calls f
  but catches and logs all exceptions."
  [f]
  (fn []
    (try (f)
         (catch Throwable e
           (log/error "Thrown in background thread: " e)))))

(defn add-shutdown-hook [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(do (println "shuting down...") (f)))))

(defn schedule-periodic
  "Schedules function f to run every 'delay' milliseconds after an
  initial delay of 'initial-delay'."
  [f initial-delay delay]
  (.scheduleWithFixedDelay (thread-pool)
                           (protect-from-exceptions f)
                           initial-delay delay TimeUnit/MILLISECONDS)
  (add-shutdown-hook f))

(defn shutdown-scheduler
  "Terminates all periodic tasks."
  []
  {:post [(nil? @pool)]}
  (swap! pool (fn [p] (when p (.shutdown p)))))