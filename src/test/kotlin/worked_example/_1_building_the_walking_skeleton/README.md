# 1. BUILDING THE WALKING SKELETON
Until we have an application that can be deployed, we cannot create any business value. The Walking Skeleton
model dictates that putting the most trivial endpoint into a production environment will prove our deployment
pipeline is sound, and helps to set the direction for the testing strategy that we will use going forward.

We start with in ICT (In-Container-Test), which have the job of testing server-level concerns such as monitoring,
documentation, and checking in a high-level way that the business endpoints are wired correctly.

## REQUIREMENTS:
- The service can be pinged over HTTP to prove that is still alive.
