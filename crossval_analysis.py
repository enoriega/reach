import re
from sys import argv
from os import path

def inspect_results(path):
    with open(path) as f:
        lines = f.readlines()

    counts_re = re.compile(r"Finished attaining (?P<successes>\d+) successes and (?P<failures>\d+) failures")
    papers_read_re = re.compile(r"Unique papers read: (?P<papers>\d+)")
    queries_re = re.compile(r"# of queries: (?P<queries>\d+)")

    for ix, line in enumerate(lines):

        m = counts_re.search(line)

        if m:
            successes = int(m.group("successes"))
            failures = int(m.group("failures"))
            continue

        m = papers_read_re.search(line)
        if m:
            papers_read = int(m.group("papers"))
            continue
        m = queries_re.search(line)
        if m:
            queries = int(m.group("queries"))
            continue

    return successes, failures, papers_read, queries



if __name__ == '__main__':
    dir = argv[1]
    file_name = path.join(dir, "testing_output{}.txt")
    for ix in range(1,4):
        print(f"Fold {ix}")
        successes, failures, papers_read, queries = inspect_results(file_name.format(ix))
        print(f"Successes: {successes}")
        print(f"Papers read: {papers_read}")
        print(f"Queries: {queries}")
        ratio = successes / papers_read
        print(f"Ratio: {ratio}")
