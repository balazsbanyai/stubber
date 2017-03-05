package com.banyaibalazs.stubber

import org.gradle.api.artifacts.Dependency;

public class BaseNameResolver {
    static String resolveName(Dependency dependency) {
        [dependency.group, dependency.name, dependency.version]
                .collect({ it -> it.tokenize('.').collect({it2 -> it2.capitalize()}).join() })
                .collect({ it -> it.capitalize() })
                .join()
    }
}
