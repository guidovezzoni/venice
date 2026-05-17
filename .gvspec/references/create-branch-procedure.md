# Create Branch Procedure

Given a user story reference, create a feature branch following these criteria:

1. The new branch should be branched off from main. If the current branch is not main, then check if there are active changes and inform the user about current branch and changes and ask what they want to do: DO NOT MAKE ASSUMPTIONS and DO NOT DELETE DATA
2. Once on main, ensure it is up-to-date with the remote by fetching and pulling the latest changes. If the pull fails or there are conflicts, inform the user and ask how to proceed: DO NOT MAKE ASSUMPTIONS
3. The new branch should live under the "feature" folder
4. The new branch start with the ticket number or reference of the user story
