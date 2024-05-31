# Extra-Exam Quiz
This Play Framework app generates a random quiz, based on the FCC question pool
for the Amateur Extra amateur radio license.  It randomly chooses one question
from each group of questions in the pool, and randomly orders the answers --
except when one of the answers must be last, like "All of the above".  It then
presents the quiz to the user.  If the user gets a question wrong, the wrong
answer will be tabulated, and the correct answer displayed to the user.  There
is no time limit imposed on completion of the quiz.

## Building
This is an sbt project, so sbt is used to build it.  It is packaged using
**SBT Native Packager**, the Docker Plugin.  To deploy the built project
as a docker image, run:

`$ sbt clean docker:publishLocal`

This will deploy as a docker image on the local machine.