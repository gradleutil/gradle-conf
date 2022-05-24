package net.gradleutil.config

import net.gradleutil.conf.util.Inflector
import spock.lang.Specification

class InflectorTest extends Specification {

    def "test inflector inflects"() {
        setup:
        def version
        def inflector = Inflector.instance

        when:
        version = inflector.pluralize('sheet')
        then:
        version == 'sheets'

        when:
        version = inflector.singularize('sheets')
        then:
        version == 'sheet'

    }


}
