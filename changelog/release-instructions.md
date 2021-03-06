# Instructions

Create a file with matching tag name that you want to publish

You can keep modifying the file with multiple pushes

Once a build is finalised, tag it with "v". For example `v2.0.0-M1`

Then create a new github release with this existing tag name.

You do not need to add any release notes or files manually in github release.

Once you publish the release, release pipeline will run and add release notes and binary file to this release. Readme will always point to latest release.

**NOTE:** It is not mandatory to first create tag manually and then create a release. But it is safer as it avoid chances of an unexpected release if some commit happens after you finalise on a build and before github release is published.