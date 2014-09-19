(ns lights-out.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]) 
  (:require
    [figwheel.client :as fw]
    [om.core :as om :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [om-tools.dom :as dom :include-macros true]
    [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(fw/watch-and-reload
 :jsload-callback (fn []
                    ;; (stop-and-start-my app)
                    ))

(defn toggle-light [state light]
  (update-in state [light] not))

(defn toggle-lights [state lights]
  (reduce toggle-light state lights))

(defonce board-state
  (atom
    (let [all-off (into {} (for [x (range 5) y (range 5)] [[x y] false]))]
      (toggle-lights all-off [

  [0 0]                   [0 4]
        [1 1] [1 2] [1 3]
        [2 1] [2 2] [2 3]
        [3 1] [3 2] [3 3]
  [4 0]                   [4 4]
                              
  ]))))

(defn find-neighbors [[x y]]
  (for 
    [n [[x (dec y)]
        [(dec x) y]
        [x y]
        [(inc x) y]
        [x (inc y)]]
     :when (contains? @board-state n)]
    n))

(defcomponent light [state _]
  (render-state [_ {:keys [toggle-chan]}]
          (let [[location lit] state
                color (if lit "red" "black")] 
            (dom/td
              (dom/div {:style {:border "solid"
                                :background-color color
                                :width "50px"
                                :height "50px"}
                        :on-click (fn [_] (put! toggle-chan location))})))))

(defcomponent board [state owner]
  (init-state [_]
              {:toggle-chan (chan)} )
  (will-mount [_]
              (let [toggle-chan (om/get-state owner :toggle-chan)]
                (go-loop []
                         (let [light (<! toggle-chan)
                               neighbors (find-neighbors light)]
                           (om/transact! state
                                         (fn [s] (toggle-lights s neighbors)))
                           (recur)))))
  (render-state [_ {:keys [toggle-chan]}]
                (dom/table
                  (for [x (range 5)]
                    (dom/tr
                      (for [y (range 5)]
                        (om/build light (find state [x y])
                                  {:init-state {:toggle-chan toggle-chan}})))))))

(om/root
  board
  board-state
  {:target (. js/document -body)})
