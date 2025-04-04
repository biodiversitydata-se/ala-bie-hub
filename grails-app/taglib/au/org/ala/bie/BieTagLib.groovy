/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.bie

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringEscapeUtils
import org.gbif.nameparser.PhraseNameParser
import org.springframework.web.servlet.support.RequestContextUtils

import java.text.MessageFormat

class BieTagLib implements GrailsConfigurationAware {
    static namespace = 'bie'     // namespace for headers and footers

    def languages

    @Override
    void setConfiguration(Config config) {
        JsonSlurper slurper = new JsonSlurper()
        URL cu = this.class.getResource(config.languageCodesUrl) ?: new URL(config.languageCodesUrl)
        def ld = slurper.parse(cu)
        languages = [:]
        ld.codes.each { code ->
            if (languages.containsKey(code.code))
                log.warn "Duplicate language code ${code.code}"
            languages[code.code] = code
            def lc = code.code.toLowerCase()
            if (lc != code.code && !languages.containsKey(lc))
                languages[lc] = code
            if (code.part2b && !languages.containsKey(code.part2b))
                languages[code.part2b] = code
            if (code.part2t && !languages.containsKey(code.part2t))
                languages[code.part2t] = code
            if (code.part1 && !languages.containsKey(code.part1))
                languages[code.part1] = code
        }
    }

    /**
     * Format a scientific name with appropriate italics depending on rank
     *
     * @attr nameFormatted OPTIONAL The HTML formatted scientific name
     * @attr nameComplete OPTIONAL The complete, unformatted scientific name
     * @attr acceptedName OPTIONAL The accepted name
     * @attr name REQUIRED the scientific name
     * @attr rankId REQUIRED The rank ID
     * @attr taxonomicStatus OPTIONAL The taxonomic status (Use "name" for a plain name and "synonym" for a plain synonym with accepted name)
     */
    def formatSciName = { attrs ->
        def nameFormatted = attrs.nameFormatted
        def rankId = attrs.rankId ?: 0
        def name = attrs.nameComplete ?: attrs.name
        def rank = cssRank(rankId)
        String accepted = attrs.acceptedName
        def taxonomicStatus = attrs.taxonomicStatus
        def parsed = { n, r, incAuthor ->
            PhraseNameParser pnp = new PhraseNameParser()
            try {
                def pn = pnp.parse(n) // attempt to parse phrase name
                log.debug "format name = ${n} || canonicalNameWithMarker = ${pn.canonicalNameWithMarker()} || incAuthor = ${incAuthor}"
                def author = incAuthor ? " <span class=\"author\">${pn.authorshipComplete()}</span>" : ""
                n = "<span class=\"scientific-name rank-${r}\"><span class=\"name\">${pn.canonicalNameWithMarker()}</span>${author}</span>"
            } catch (Exception ex) {
                log.warn "Error parsing name (${n}): ${ex}"
            }
            n
        }
        if (!taxonomicStatus)
            taxonomicStatus = accepted ? "synonym" : "name"
        def format = message(code: "taxonomicStatus.${taxonomicStatus}.format", default: "<span class=\"taxon-name\">{0}</span>")
        if (!nameFormatted) {
            def output = "<span class=\"scientific-name rank-${rank}\"><span class=\"name\">${name}</span></span>"
            if (rankId >= 6000)
                output = parsed(name, rank, true)
            nameFormatted = output
        }
        if (accepted && accepted.tokenize(" ").size() < 7) {
            accepted = parsed(accepted, rank, false)
        }
        out << MessageFormat.format(format, nameFormatted, accepted)
    }

    /**
     * Constructs a link to EYA from this locality.
     */
    def constructEYALink = {  attrs, body ->

        def group = attrs.result.centroid =~ /([\d.-]+) ([\d.-]+)/
        def bieUrl = grailsApplication.config.biocache.baseURL

        def parsed = group && group[0] && group[0].size() == 3
        if(parsed){
            def latLong = group[0]
            out <<  "<a href='" + bieUrl + "/explore/your-area#" +
                    latLong[2] + "|" + latLong[1] + "|12|ALL_SPECIES'>"
        }

        out << body()

        if(parsed){
            out << "</a>"
        }
    }

    /**
     * Output the colour name for a given conservationstatus
     *
     * @attr status REQUIRED the conservation status
     */
    def colourForStatus = { attrs ->
//        <g:if test="${status.status ==~ /extinct$/}"><span class="iucn red"><g:message code="region.${regionCode}"/><!--EX--></span></g:if>
//        <g:elseif test="${status.status ==~ /(?i)wild/}"><span class="iucn red"><g:message code="region.${regionCode}"/><!--EW--></span></g:elseif>
//        <g:elseif test="${status.status ==~ /(?i)Critically/}"><span class="iucn yellow"><g:message code="region.${regionCode}"/><!--CR--></span></g:elseif>
//        <g:elseif test="${status.status ==~ /(?i)^Endangered/}"><span class="iucn yellow"><g:message code="region.${regionCode}"/><!--EN--></span></g:elseif>
//        <g:elseif test="${status.status ==~ /(?i)Vulnerable/}"><span class="iucn yellow"><g:message code="region.${regionCode}"/><!--VU--></span></g:elseif>
//        <g:elseif test="${status.status ==~ /(?i)Near/}"><span class="iucn green"><g:message code="region.${regionCode}"/><!--NT--></span></g:elseif>
//        <g:elseif test="${status.status ==~ /(?i)concern/}"><span class="iucn green"><g:message code="region.${regionCode}"/><!--LC--></span></g:elseif>
//        <g:else><span class="iucn green"><g:message code="region.${regionCode}"/><!--LC--></span></g:else>
        def status = attrs.status
        def colour

        switch ( status ) {
            case ~/(?i)extinct/:
                colour = "extinct"
                break
            case ~/(?i).*extinct.*/:
                colour = "black"
                break
            case ~/(?i)critically\sendangered.*/:
                colour = "red"
                break
            case ~/(?i)endangered.*/:
                colour = "orange"
                break
            case ~/(?i)vulnerable.*/:
                colour = "yellow"
                break
            case ~/(?i)near\sthreatened.*/:
                colour = "near-threatened"
                break
                //case ~/(?i)least\sconcern.*/:
            default:
                colour = "green"
                break
        }

        out << colour
    }

    /**
     * Tag to output the navigation links for search results
     *
     *  @attr totalRecords REQUIRED
     *  @attr startIndex REQUIRED
     *  @attr pageSize REQUIRED
     *  @attr lastPage REQUIRED
     *  @attr title
     */
    def searchNavigationLinks = { attr ->
        log.debug "attr = " + attr
        def lastPage = attr.lastPage?:1
        def pageSize = attr.pageSize?:10
        def totalRecords = attr.totalRecords
        def startIndex = attr.startIndex?:0
        def title = attr.title?:""
        def pageNumber = (attr.startIndex / attr.pageSize) + 1
        def trimText = params.q?.trim()
        def fqList = params.list("fq")
        def coreParams = (fqList) ? "?q=${trimText}&fq=${fqList.join('&fq=')}" : "?q=${trimText}"
        def startPageLink = 0
        if (pageNumber < 6 || attr.lastPage < 10) {
            startPageLink = 1
        } else if ((pageNumber + 4) < lastPage) {
            startPageLink = pageNumber - 4
        } else {
            startPageLink = lastPage - 8
        }
        if (pageSize > 0) {
            lastPage = (totalRecords / pageSize) + ((totalRecords % pageSize > 0) ? 1 : 0);
        }
        def endPageLink = (lastPage > (startPageLink + 8)) ? startPageLink + 8 : lastPage

        // Uses MarkupBuilder to create HTML
        def mb = new groovy.xml.MarkupBuilder(out)
        mb.ul {
            li(id:"prevPage") {
                if (startIndex > 0) {
                    mkp.yieldUnescaped("<a href=\"${coreParams}&start=${startIndex - pageSize}&title=${title}\">&laquo; Previous</a>")
                } else {
                    mkp.yieldUnescaped("<span>&laquo; Previous</span>")
                }
            }
            (startPageLink..endPageLink).each { pageLink ->
                if (pageLink == pageNumber) {
                    mkp.yieldUnescaped("<li class=\"currentPage\">${pageLink}</li>")
                } else {
                    mkp.yieldUnescaped("<li><a href=\"${coreParams}&start=${(pageLink * pageSize) - pageSize}&title=${title}\">${pageLink}</a></li>")
                }
            }
            li(id:"nextPage") {
                if (!(pageNumber == endPageLink)) {
                    mkp.yieldUnescaped("<a href=\"${coreParams}&start=${startIndex + pageSize}&title=${title}\">Next &raquo;</a>")
                } else {
                    mkp.yieldUnescaped("<span>Next &raquo;</span>")
                }
            }
        }
    }

    /**
     * Mark a phrase with language, optionally with a specific language marker
     * <p>
     * This can be done two ways, one as a semantic tag.
     *
     * @attr text The text to mark
     * @attr lang The language code (ISO) (defaults to the request locale or the default locale)
     * @attr mark Mark the language in text (defaults to true)
     * @attr href Link to the text
     * @attr tag Use a tag marker, if false, use "in *language*" (defaults to true)
     */
    def markLanguage = { attrs ->
        Locale defaultLocale = RequestContextUtils.getLocale(request)
        String text = attrs.text ?: ""
        Locale lang = buildLocale(attrs.lang)
        String href = attrs.href
        boolean mark = attrs.containsKey('mark') ? attrs.mark : true
        boolean tag = attrs.containsKey('tag') ? attrs.tag : true

        out << "<span lang=\"${lang}\">"
        if (href)
            out << "<a href=\"${href}\" target=\"_blank\" class=\"external\">${text}</a>"
        else
            out << text
        if (mark && defaultLocale.language != lang.language) {
            def name = languages[lang.language] ?: [code: lang.toLanguageTag(), name: lang.displayName]
            if (tag) {
                out << "&nbsp;<span class=\"annotation annotation-language\" title=\"${name.code}\">${name.name}</span>"
            } else {
                String inLabel = message(code: 'label.in', default: 'in')
                out << "<span class=\"in-marker\">&nbsp;${inLabel}&nbsp;</span><span class=\"language-name\" title=\"${lang.language}\">${name.name}</span>"
            }
        }
        out << "</span>"
    }

    /**
     * Show language information, including a link to the language definition, if available
     *
     * @attr lang REQUIRED The language code
     * @attr format Format into HTML (true by default)
     * @attr default The default language name, if not found
     */
    def showLanguage = { attrs ->
        Locale lang = buildLocale(attrs.lang)
        boolean format = attrs.containsKey('format') ? attrs.format : true
        def name = languages[lang.language]
        def display = name?.name ?: attrs.default ?: lang.displayName
        if (!format) {
            out << display
        } else {
            out << "<span class=\"language-name\" title=\"${name?.code ?: lang.language}\">"
            if (name.uri)
                out << "<a href=\"${name.uri}\" target=\"_blank\">${display}</a>"
            else
                out << display
            out << "</span>"
        }
    }

    /**
     * Mark a common name with status information
     *
     * @attr status The status term
     * @attr tags tags, separated by a |
     */
    def markCommonStatus = { attrs ->
        String status = attrs.status ?: "common"
        List<String> tags = attrs.tags?.split("\\|")

        if (status != "common" || tags) {
            String statusDetail = message(code: "commonStatus.${status}.detail", default: '')
            String statusText = message(code: "commonStatus.${status}", default: status)
            out << "<span class=\"annotation annotation-status\" title=\"${statusDetail}\">${statusText}</span>"
            tags.each {
                String labelCode = it.trim().replaceAll("\\W", "-").toLowerCase()
                String labelDetail = message(code: "tag.${labelCode}.detail", default: '')
                String labelText = message(code: "tag.${labelCode}", default: it)
                out << "&nbsp;<span class=\"tag tag-${labelCode}\" title=\"${labelDetail}\">${labelText}</span>"
            }
        }

    }

    /**
     * Convert a country code into a country name
     *
     * @attr code The country code
     * @attr lang The language to use (defaults to the request locale or the default locale)
     */
    def country = { attrs ->
        if (!attrs.code)
            return
        Locale defaultLocale = RequestContextUtils.getLocale(request)
        Locale lang = Locale.forLanguageTag(attrs.lang ?: defaultLocale.language)
        Locale country = new Locale(lang.language, attrs.code)
        out << "<span lang=\"${lang.toLanguageTag()}\">${country ? country.getDisplayCountry(lang) : attrs.code}</span>"
    }

    def displaySearchHighlights = {  attrs, body ->
        if(attrs.highlight) {
            def parts = attrs.highlight.split("<br>")
            //remove duplicates
            def cleaned = [:]
            parts.each {
                def cleanedKey = it.replaceAll("</b>", "").replaceAll("<b>", "")
                if(!cleaned.containsKey(cleanedKey)){
                    cleaned.put(cleanedKey, it)
                }
            }
            cleaned.eachWithIndex { entry, index ->
                if(index > 0){
                    out << "<br/>"
                }
                out << entry.value
            }
        }
    }

    /**
     * Custom function to fetch a AusTraits message from message.properties and formatting to add links to AusTraits keyword
     */
    def ausTraitsLinkedDescription = {attrs, body   ->
        def messageType  = attrs.message
        def messageKey = "aus.traits.${messageType}"
        def msg  = g.message(code: messageKey) as Object
        def linkText = "AusTraits"
        def replaceMent  = "<a href=\"${grailsApplication.config.ausTraits.homeURL}\" target=\"_blank\">${linkText}</a>"


        out <<  msg.toString().replaceAll(linkText, replaceMent)

    }

    /**
     * Custom function to escape a string for JS use
     *
     * @param value
     * @return
     */
    def static escapeJS(String value) {
        return StringEscapeUtils.escapeJavaScript(value);
    }

    /**
     * Get a default rank grouping for a rank ID.
     * <p>
     * See bie-index taxonRanks.properties for the rank structure
     *
     * @param rankId The rank identifier
     *
     * @return The grouping
     */
    private def cssRank(int rankId) {
        if (rankId <= 0)
            return "unknown"
        if (rankId <= 1200)
            return "kingdom"
        if (rankId <= 2200)
            return "phylum"
        if (rankId <= 3400)
            return "class"
        if (rankId <= 4400)
            return "order"
        if (rankId <= 5700)
            return "family"
        if (rankId < 7000)
            return "genus"
        if (rankId < 8000)
            return "species"
        return "subspecies"
    }

    /**
     * Build a locale.
     *
     * If it's well-known locale, then use that.
     * Otherwise, build a new locale from the language definition
     *
     * @param lang The language code
     *
     * @return A corresponding locale
     */
    private buildLocale(String lang) {
        def locale = Locale.forLanguageTag(lang ?: request.locale.language)
        if (!locale || !locale.language) {
            locale = new Locale(lang)
        }
        return locale
    }
}