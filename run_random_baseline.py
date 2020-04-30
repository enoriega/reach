
import os
import os.path as path
from tqdm import tqdm
from glob import glob

print("Performig Random Baselines")

DATADIR = "/Users/enrique/Research/focused_reading/crossval/"
TESTING_FILES = "test1.txt test2.txt test3.txt".split()

for explore_weight in [0.0, 0.10, 0.25, 0.5, 0.75, 1.0]:
    for trial in range(1, 6):
        for ix, testing_path in enumerate(TESTING_FILES):
            ix += 1
            print(f"Trial {trial}, fold {ix}, explore weight {explore_weight}")
            testing_path = path.join(DATADIR, testing_path)
            testing_output = f"random_policy_{ix}_{trial}_{explore_weight}.txt"

            testing_cmd = f"sbt -J-Xmx12g -DDyCE.Testing.file={testing_path} -DDyCE.Random.exploreWeight={explore_weight} \"runMain org.clulab.reach.focusedreading.executable.SimplePath\""

            with open(testing_output, 'w') as ftest:
                # print("Runnin random baseline ...")
                ftest.write(os.popen(testing_cmd).read())
                # print(testing_cmd)
            print()

print("Done!")
