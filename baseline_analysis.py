import os.path
from glob import glob
from crossval_analysis import inspect_results
import re
from collections import defaultdict
import pandas as pd
import matplotlib.pyplot as plt

if __name__ == '__main__':
    data_dir = os.path.join("cross_val_results", "random_baseline_trials_weighted")
    w_paths = glob(os.path.join(data_dir, "random_policy_*.txt"))
    random_paths = glob(os.path.join("cross_val_results", "random_baseline_trials", "random_policy_*"))

    # First random baseline
    trials = defaultdict(list)
    for p in random_paths:
        fold = int(p.split(os.path.sep)[-1].split("_")[2])
        successes, _, papers, _ = inspect_results(p)
        ratio = successes/papers
        trials[fold].append(ratio)

    f = pd.DataFrame([{'fold': k, 'ratio': v} for k, vs in trials.items() for v in vs])

    plt.ion()

    f.boxplot(by='fold')
    plt.xlabel("Fold")
    plt.ylabel("Ratio")
    plt.show()

    # Then the wikihop fr style plot
    data = list()
    for p in w_paths:
        m = re.search(r"random_policy_(?P<fold>\d)_\d_(?P<explore>[\d\.]+).txt$", p)
        fold, p_explore = int(m.group("fold")), float(m.group("explore"))
        successes, _, papers, _ = inspect_results(p)
        data.append({"fold": fold, "p_explore": p_explore, "papers": papers, "successes": successes})

    f = pd.DataFrame(data)

    for fold in range(1, 4):
        plt.figure()
        plt.title(f"Baseline Performance Comparison - Fold {fold}")
        plt.xlabel("Total papers read")
        plt.ylabel("Successes rate")
        for p_explore in sorted(f.p_explore.unique()):
            x = f[(f.fold == fold) & (f.p_explore == p_explore)]
            plt.scatter(x.papers, x.successes, label=p_explore)
        # plt.ylim(0, 1)
        plt.legend()
        plt.grid(True)
        plt.show()

    input()
