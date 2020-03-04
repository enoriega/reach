
import os
import os.path as path

print("Performig Cross Validation")

DATADIR = "/Users/enrique/Research/focused_reading/crossval/"
TRAINING_FILES = "train1.txt train2.txt train3.txt".split()
TESTING_FILES = "test1.txt test2.txt test3.txt".split()

for ix, (training_path, testing_path) in enumerate(zip(TRAINING_FILES, TESTING_FILES)):
    ix += 1
    print(f"Fold {ix}")
    training_path = path.join(DATADIR, training_path)
    testing_path = path.join(DATADIR, testing_path)
    policy_file = f"cross_val_policy_{ix}.json"
    training_output = f"training_output{ix}.txt"
    testing_output = f"testing_output{ix}.txt"

    training_cmd = f"sbt -DDyCE.Training.file={training_path} -DDyCE.Training.policyFile={policy_file} \"runMain org.clulab.reach.focusedreading.reinforcement_learning.exec.focused_reading.LinearSARSA\""
    testing_cmd = f"sbt -DDyCE.Testing.file={training_path} -DDyCE.Testing.policyFile={policy_file} \"runMain org.clulab.reach.focusedreading.executable.SimplePathRL\""

    with open(training_output, 'w') as ftrain, open(testing_output, 'w') as ftest:
        print("Training ...")
        ftrain.write(os.popen(training_cmd).read())
        print("Testing ...")
        ftest.write(os.popen(testing_cmd).read())
    # os.system(training_cmd)
    # os.system(testing_cmd)
    print()

print("Done!")
