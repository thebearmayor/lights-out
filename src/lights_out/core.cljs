(ns lights-out.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]) 
  (:require
    [figwheel.client :as fw]
    [om.core :as om :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [om-tools.dom :as dom :include-macros true]
    [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload
;; (defonce app-data (atom {}))

(defonce board-state
  (atom
    {[0 0] {:lit true} [0 1] {:lit false} [0 2] {:lit false} [0 3] {:lit true}
     [1 0] {:lit false} [1 1] {:lit true} [1 2] {:lit true} [1 3] {:lit false}
     [2 0] {:lit false} [2 1] {:lit true} [2 2] {:lit true} [2 3] {:lit false}
     [3 0] {:lit true} [3 1] {:lit false} [3 2] {:lit false} [3 3] {:lit true}
     }))

(fw/watch-and-reload
 :jsload-callback (fn []
                    ;; (stop-and-start-my app)
                    ))

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
          (let [[location {:keys [lit]}] state
                color (if lit "red" "black")
                ] 
            (dom/td
              (dom/div {:style {:border "solid"
                                :background-color color
                                :width "50px"
                                :height "50px"}
                        :on-click (fn [_] (put! toggle-chan location))})))))

(defn toggle-light [state light]
  (update-in state [light :lit] not))

(defn toggle-lights [state lights]
  (reduce toggle-light state lights))

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
                  (for [x (range 4)]
                    (dom/tr
                      (for [y (range 4)]
                        (om/build light (find state [x y])
                                  {:init-state {:toggle-chan toggle-chan}})))))))

(om/root
  board
  board-state
  {:target (. js/document -body)})
