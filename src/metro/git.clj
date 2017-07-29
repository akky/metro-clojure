(ns metro.git
  (:require [clojure.string :as str]))

(defn git-checkout
  [branch repo]
  (if (contains? (set (keys repo)) branch)
    (str "git checkout " branch)
    (str "git checkout --orphan " branch)))

(defn git-commit
  [commit-name]
  (str "git commit --allow-empty -m \"" commit-name "\""))

(defn git-force-branch
  [branches]
  (map (fn [branch] (str "git branch -f " branch " HEAD")) branches))

(defn git-merge
  [commit-name branches]
  (str "git merge --strategy=ours --allow-unrelated-histories --no-ff --commit -m \""
       commit-name
       "\" "
       (str/join " " branches)))

(defn pick-head
  [head repo branches]
  (if (and
       (contains? (set branches) head)
       (contains? (set (keys repo)) head))
    head
    (first branches)))

(defn find-divergent-branches
  [head repo branches]
  (let [station (get repo head)]
    (filter
     (fn [branch]
       (let [branch-station (get repo branch)]
         (and
          (not (nil? branch-station))
          (not= branch-station station)
          (not= branch head))))
     branches)))

(defn find-remaining-branches
  [head merging-branches branches]
  (->> (clojure.set/difference (set branches) (set merging-branches))
       (remove #{head}))) 

(defn update-repo
  [repo branches commit-name]
  (into repo (map (fn [branch] {branch commit-name}) branches)))

(defn create-git-commands
  [state station]
  (let [commit-name (:station station)
        branches (:line station)
        repo (:repo state)
        head (:head state)
        observer (:observer state)
        new-head (pick-head head repo branches)]

    ;; checkout to the branch
    (if-not (= head new-head)
      (observer (git-checkout new-head repo)))

    ;; check if branch has more than one pointing to new-head
    (let [merging-branches (find-divergent-branches new-head repo branches)
          remaining-branches (find-remaining-branches new-head merging-branches branches)]
      (if (> (count merging-branches) 0)
          (observer (git-merge commit-name merging-branches))
          (observer (git-commit commit-name)))

      (when (not-empty merging-branches)
        (run! observer (git-force-branch merging-branches)))

      (when (not-empty remaining-branches)
        (run! observer (git-force-branch remaining-branches))))

    (assoc state :head new-head :repo (update-repo repo branches commit-name))))

(defn build-git-operations
  [subway-seq observer]
  (let [initial-state {:repo {} :observer observer}]
    (reduce create-git-commands initial-state subway-seq)
    {}))
