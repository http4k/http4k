# 2. ADDING THE FIRST BUSINESS-LEVEL STORY.
Starting with another EndToEnd test, we can then drill-down into the functional behaviour of the system by introducing
OCT (Out of Container) tests and converting the e2e test to just test endpoint wiring (so far). The common assertions have
also been converted to reusable extension methods on Response.

## REQUIREMENTS:
- Implement an "add" service, which will sum a number of integer values.
