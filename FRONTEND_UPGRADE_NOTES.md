# Frontend revision

The original frontend was converted into a responsive same-origin web interface for patient, doctor, and administrator workflows.

Key changes:

- modern shared visual theme;
- live patient, doctor, and administrator dashboard data;
- role guards and consistent session redirects;
- demo OTP login;
- relative `/api` paths for local and deployed use;
- escaped dynamic dashboard data;
- corrected duplicate IDs and broken scripts.

The frontend remains intentionally framework-free to keep the project understandable for a university oral exam. A future version could migrate to React or another component framework after automated end-to-end tests are in place.
