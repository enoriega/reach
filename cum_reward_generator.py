
import os
import os.path as path
from tqdm import tqdm
from glob import glob

print("Performig Cross Validation")

DATADIR = "/Users/enrique/Research/focused_reading/crossval/"
TRAINING_FILES = "train1.txt train2.txt train3.txt".split()

for ix, training_path in enumerate(TRAINING_FILES):
    ix += 1
    for trail in range(1, 11):
        print(f"Fold {ix}, Trial {trail}")
        training_path = path.join(DATADIR, training_path)
        policy_file = f"cross_val_policy_{ix}.json"
        training_output = f"training_output{ix}.txt"
        cum_rewards_file = f"epoch_rewards_fold{ix}_{trail}.txt"

        training_cmd = f"sbt -J-Xmx24g -DDyCE.Training.file={training_path} -DDyCE.Training.epochs=10520 -DDyCE.Training.cumRewardsFile={cum_rewards_file} -DDyCE.Training.policyFile={policy_file} \"runMain org.clulab.reach.focusedreading.reinforcement_learning.exec.focused_reading.LinearSARSA\""

        with open(training_output, 'w') as ftrain:
            print("Training ...")
            # ftrain.write(os.popen(training_cmd).read())
            print(training_cmd)

        print()

print("Done!")
