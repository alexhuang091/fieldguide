/**
 * Copyright (c) CSIRO Australia, 2009
 * All rights reserved.
 *
 * Original Author: hwa002
 * Last Modified By: $LastChangedBy: hwa002 $
 * Last Modified Info: $Id: AbrsFloraOfOzOnlineDocumentMapper.java 2114 2009-12-16 23:09:34Z hwa002 $
 */

package org.ala.documentmapper;

import java.util.ArrayList;
import java.util.List;

import org.ala.repository.ParsedDocument;
import org.ala.repository.Predicates;
import org.ala.repository.Triple;
import org.ala.util.MimeType;
import org.w3c.dom.Document;

/**
 * Document Mapper for Ladybirds of Australia 
 *
 * @author Tommy Wang (tommy.wang@csiro.au)
 */
public class LoAHtmlDocumentMapper extends XMLDocumentMapper {

	public LoAHtmlDocumentMapper() {

		setRecursiveValueExtraction(true);

		this.contentType = MimeType.HTML.toString();

		String subject = MappingUtils.getSubject();

		// Extracts the unique ID from supplied page.
		// Unique ID is assumed to be embedded inside a <meta> element
		// generated by Protocol Handler.
		// Location is a HTML <meta> tag in /html/head
		addDCMapping("//html/head/meta[@scheme=\"URL\" and @name=\"ALA.Guid\"]/attribute::content", subject, Predicates.DC_IDENTIFIER);

		// Extracts the entire taxon name string
		// According to TDWG's TaxonName standard
		// http://rs.tdwg.org/ontology/voc/TaxonName#nameComplete
		// "Every TaxonName should have a DublinCore:title property that contains
		// the complete name string including authors and year (where appropriate)."
		
		addDCMapping("//html/body/div[1]/table/tbody/tr/td[2][@id=\"italicTitle\"]/text()", subject, Predicates.DC_TITLE);
		
		addTripleMapping("//html/body/div[1]/table/tbody/tr/td[2][@id=\"italicTitle\"]/text()", subject, Predicates.SCIENTIFIC_NAME);
		
		addTripleMapping("//p/span[a[contains(.,\"Synonyms\")]]/following-sibling::span[1]/text()", subject, Predicates.SYNONYM);
		
		addTripleMapping("//p[@id=\"map\"]", subject, Predicates.DISTRIBUTION_TEXT);
		
		addTripleMapping("//p[@id=\"map\"]/a/attribute::href", subject, Predicates.DIST_MAP_IMG_URL);
		
		addTripleMapping("//p/span[a[contains(.,\"Description\")]]/following-sibling::text()", subject, Predicates.DESCRIPTIVE_TEXT);
		
	} // End of default constructor.


	@Override
	protected void extractProperties(List<ParsedDocument> pds, Document xmlDocument) throws Exception {

		ParsedDocument pd = pds.get(0);
		List<Triple<String,String,String>> triples = pd.getTriples();
		
		List<Triple<String,String,String>> toRemove = new ArrayList<Triple<String,String,String>>();
		
		String subject = MappingUtils.getSubject();
		
//		pd.getDublinCore().put(Predicates.DC_CREATOR.toString(), "Ladybirds of Australia");
		pd.getDublinCore().put(Predicates.DC_LICENSE.toString(), "http://www.csiro.au/org/LegalNoticeAndDisclaimer.html");
		
		String source = "http://www.ento.csiro.au/biology/ladybirds/";
		
		for (Triple<String,String,String> triple: triples) {
			String predicate = triple.getPredicate().toString();
			if(predicate.endsWith("hasDistributionMapImageUrl")) {
				String imageUrl = (String) triple.getObject();
				imageUrl = imageUrl.replaceAll("\\.\\./", "");
				imageUrl = source + imageUrl;
				
				ParsedDocument imageDoc = MappingUtils.retrieveImageDocument(pd, imageUrl);
				if(imageDoc!=null){
					List<Triple<String,String,String>> imageDocTriples = imageDoc.getTriples();
					imageDocTriples.add(new Triple(subject,Predicates.DIST_MAP_IMG_URL.toString(), imageDoc.getGuid()));
					imageDoc.setTriples(imageDocTriples);
					pds.add(imageDoc);
				}
				
				//remove the triple from the triples
				toRemove.add(triple);
			}
			
			if(triple.getObject()!=null){
				String objectValue = triple.getObject().toString();
				if(objectValue.contains("UNDER CONSTRUCTION")){
					toRemove.add(triple);
				}
			}
		}
		
		triples.removeAll(toRemove);
		
		String imageUrl = pd.getGuid();
		
		imageUrl = imageUrl.replace("1.htm", "Dor.jpg");
		imageUrl = imageUrl.replace("lucid/key/lucidKey/Media/Html", "big_images/species");
		
		ParsedDocument imageDoc = MappingUtils.retrieveImageDocument(pd, imageUrl);
		if(imageDoc!=null){
			pds.add(imageDoc);
		}
		
		triples.add(new Triple(subject, Predicates.KINGDOM.toString(), "Animalia"));
				
		pd.setTriples(triples);

	} // End of `postProcessProperties` method.
}
