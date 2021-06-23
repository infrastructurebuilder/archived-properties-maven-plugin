# MojoHaus Properties Maven Plugin

This was forked from the original properties-maven-plugin to fix issue mojohaus/properties-maven-plugin#30.
This is no longer the [properties-maven-plugin](http://www.mojohaus.org/properties-maven-plugin/).


## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```

