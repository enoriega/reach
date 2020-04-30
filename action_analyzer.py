import re
import sys
import matplotlib.pyplot as plt
import pandas as pd

action_pattern = re.compile(r"Chosen query: Query\((?P<action>\w+),")

def main(file):
    with open(file) as f:
        lines = f.readlines()

    actions = list()

    for line in lines:
        match = action_pattern.search(line)
        if match:
            action = match.group("action")
            actions.append(action)

    actions = pd.Series(actions)

    print(actions.value_counts())

    # plt.figure()
    # plt.title("Action distribution")
    # actions.hist()
    # plt.show()


if __name__ == "__main__":
    main(sys.argv[1])
