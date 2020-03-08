import re
import sys
import matplotlib
import matplotlib.pyplot as plt


loss_re = re.compile("Loss: (?P<loss>[\d\.\-E]+)")

def main(file):
    with open(file) as f:
        lines = f.readlines()

    loss_vals = list()
    for line in lines:
        match = loss_re.search(line)
        if match:
            loss_vals.append(float(match.group("loss")))

    print(len(loss_vals))
    plot_loss(loss_vals)

def plot_loss(vals):
    plt.figure()
    plt.title("Loss value over time")
    plt.ylabel("Loss value")
    plt.xlabel("Update")
    plt.plot(vals)
    plt.show()

if __name__ == "__main__":
    main(sys.argv[1])
