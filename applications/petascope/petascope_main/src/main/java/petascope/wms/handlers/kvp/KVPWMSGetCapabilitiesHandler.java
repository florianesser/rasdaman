/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wms.handlers.kvp;

import java.util.ArrayList;
import petascope.core.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.lang3.StringUtils;
import org.rasdaman.config.ConfigManager;
import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.domain.cis.Wgs84BoundingBox;
import org.rasdaman.domain.owsmetadata.Address;
import org.rasdaman.domain.owsmetadata.OwsServiceMetadata;
import org.rasdaman.domain.owsmetadata.Phone;
import org.rasdaman.domain.owsmetadata.ServiceIdentification;
import org.rasdaman.domain.owsmetadata.ServiceProvider;
import org.rasdaman.domain.wms.Dimension;
import org.rasdaman.domain.wms.EXGeographicBoundingBox;
import org.rasdaman.domain.wms.Layer;
import org.rasdaman.domain.wms.LayerAttribute;
import org.rasdaman.domain.wms.Style;
import org.rasdaman.domain.wms.Style.ColorTableType;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.rasdaman.repository.service.OWSMetadataRepostioryService;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.core.KVPSymbols;
import petascope.core.Templates;
import petascope.core.XMLSymbols;
import petascope.core.gml.GMLGetCapabilitiesBuilder;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.util.MIMEUtil;
import petascope.exceptions.WMSException;
import petascope.util.CrsProjectionUtil;
import petascope.util.ListUtil;
import petascope.util.XMLUtil;
import petascope.wms.exception.WMSLayerNotExistException;

/**
 * Handle the GetCapabilities WMS 1.3 request. A model result, see:
 * http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xml
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
public class KVPWMSGetCapabilitiesHandler extends KVPWMSAbstractHandler {

    private static Logger log = LoggerFactory.getLogger(KVPWMSGetCapabilitiesHandler.class);
    
    @Autowired
    private WMSRepostioryService wmsRepostioryService;
    @Autowired
    private OWSMetadataRepostioryService persistedOwsServiceMetadataService;
    @Autowired
    private GMLGetCapabilitiesBuilder wcsGMLGetCapabilitiesBuild;
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;

    private OwsServiceMetadata owsServiceMetadata;

    public KVPWMSGetCapabilitiesHandler() {

    }

    // List of output format for WMS request (only 2D)
    public static final List<String> supportedFormats = ListUtil.valuesToList(MIMEUtil.MIME_JPEG, MIMEUtil.MIME_PNG, MIMEUtil.MIME_TIFF);

    // Xml output. (The default format)
    public static final String EXCEPTION_XML = "XML";
    // INIMAGE: Generates an image
    public static final String EXCEPTION_INIMAGE = "INIMAGE";
    // BLANK: Generates a blank image
    public static final String EXCEPTION_BLANK = "BLANK";
    public static final List<String> supportedExceptions = ListUtil.valuesToList(EXCEPTION_XML, EXCEPTION_INIMAGE, EXCEPTION_BLANK);

    @Override
    public void validate(Map<String, String[]> kvpParameters) throws WMSException {

    }

    @Override
    public Response handle(Map<String, String[]> kvpParameters) throws PetascopeException, SecoreException, WMSException {
        // Validate before handling the request
        this.validate(kvpParameters);

        owsServiceMetadata = this.persistedOwsServiceMetadataService.read();
        // Build ows:ServiceIdentification element
        String service = this.buildServiceElement().toXML();
        String capabiltiy = this.buildCapability();

        // Load the template GetCapabilities result then replaces with the xml elements in string
        String getCapabilitiesTemplate = Templates.getTemplate(Templates.WMS_GET_CAPABILITIES);
        getCapabilitiesTemplate = getCapabilitiesTemplate.replace(Templates.WMS_GET_CAPABILITIES_SERVICE_ELEMENT, service);
        getCapabilitiesTemplate = getCapabilitiesTemplate.replace(Templates.WMS_GET_CAPABILITIES_CAPABILITY_ELEMENT, capabiltiy);

        // Replace all the string enquotes from getRepresentation() of inner elments
        getCapabilitiesTemplate = XMLUtil.replaceEnquotes(getCapabilitiesTemplate);

        // format XML to have indentation
        getCapabilitiesTemplate = XMLUtil.formatXML(getCapabilitiesTemplate);

        // GetCapabilities only returns 1 XML string                
        return new Response(Arrays.asList(getCapabilitiesTemplate.getBytes()), MIMEUtil.MIME_GML);
    }

    /**
     * Build the Service Element of WMS_Capabilities element
     *
     * @return
     */
    private Element buildServiceElement() {

        ServiceIdentification serviceIdentification = this.owsServiceMetadata.getServiceIdentification();
        ServiceProvider serviceProvider = this.owsServiceMetadata.getServiceProvider();
        // Service
        Element serviceElement = new Element(XMLSymbols.LABEL_WMS_SERVICE);

        // Name
        Element nameElement = new Element(XMLSymbols.LABEL_WMS_NAME);
        nameElement.appendChild(KVPSymbols.WMS_SERVICE);
        serviceElement.appendChild(nameElement);

        // Title
        Element titleElement = new Element(XMLSymbols.LABEL_WMS_TITLE);
        titleElement.appendChild(serviceIdentification.getServiceTitle());
        serviceElement.appendChild(titleElement);

        // Abstract
        Element abstractElement = new Element(XMLSymbols.LABEL_WMS_ABSTRACT);
        abstractElement.appendChild(serviceIdentification.getServiceAbstract());
        serviceElement.appendChild(abstractElement);

        // KeywordList
        if (!serviceIdentification.getKeywords().isEmpty()) {
            Element keyWordsElement = this.buildKeywordListElement(serviceIdentification.getKeywords());
            serviceElement.appendChild(keyWordsElement);
        }

        // OnlineResource
        Element onlineResourceElement = new Element(XMLSymbols.LABEL_WMS_ONLINE_RESOURCE);
        Attribute providerSiteAttribute = new Attribute(XMLSymbols.PREFIX_XLINK + ":" + XMLSymbols.ATT_HREF,
                XMLSymbols.NAMESPACE_XLINK,
                serviceProvider.getProviderSite());
        onlineResourceElement.addAttribute(providerSiteAttribute);
        serviceElement.appendChild(onlineResourceElement);

        // ContactInformation
        Element contactInformationElement = this.buildContactInformation(serviceProvider);
        serviceElement.appendChild(contactInformationElement);

        // Fees
        if (serviceIdentification.getFees() != null) {
            Element feesElement = new Element(XMLSymbols.LABEL_WMS_FEES);
            feesElement.appendChild(serviceIdentification.getFees());
            serviceElement.appendChild(feesElement);
        }

        // AccessConstraints
        if (serviceIdentification.getAccessConstraints().size() > 0) {
            Element accessContraintsElement = new Element(XMLSymbols.LABEL_WMS_ACCESS_CONSTRAINTS);
            accessContraintsElement.appendChild(serviceIdentification.getAccessConstraints().get(0));
        }

        return serviceElement;
    }

    /**
     * Build the ContactInformation element of Service element
     *
     * @param serviceProvider
     * @return
     */
    private Element buildContactInformation(ServiceProvider serviceProvider) {
        // ContactInformation        
        Element contactInformationElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_INFORMATION);

        // ContactPersonPrimary
        Element contactPersonPrimaryElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_PERSON_PRIMARY);
        contactInformationElement.appendChild(contactPersonPrimaryElement);

        // ContactPerson
        Element contactPersonElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_PERSON);
        contactPersonElement.appendChild(serviceProvider.getServiceContact().getIndividualName());
        contactPersonPrimaryElement.appendChild(contactPersonElement);
        // ContactOrganization
        Element contactOrganizationElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_ORGANIZATION);
        contactOrganizationElement.appendChild(serviceProvider.getProviderName());
        contactPersonPrimaryElement.appendChild(contactOrganizationElement);

        // ContactPosition
        Element contactPositionElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_POSITION);
        contactPositionElement.appendChild(serviceProvider.getServiceContact().getPositionName());
        contactInformationElement.appendChild(contactPositionElement);

        // ContactAddress
        Address address = serviceProvider.getServiceContact().getContactInfo().getAddress();
        Element contactAddressElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_ADDRESS);
        contactInformationElement.appendChild(contactAddressElement);

        // AddressType
        Element addressTypeElement = new Element(XMLSymbols.LABEL_WMS_ADDRESS_TYPE);
        addressTypeElement.appendChild(XMLSymbols.VALUE_WMS_ADDRES_TYPE);
        contactAddressElement.appendChild(addressTypeElement);

        // Address
        Element addressElement = new Element(XMLSymbols.LABEL_WMS_ADDRESS);
        addressElement.appendChild(address.getDeliveryPoints().get(0));
        contactAddressElement.appendChild(addressElement);

        // City
        Element cityElement = new Element(XMLSymbols.LABEL_WMS_CITY);
        cityElement.appendChild(address.getCity());
        contactAddressElement.appendChild(cityElement);

        // StateOrProvince
        Element stateOrProvinceElement = new Element(XMLSymbols.LABEL_WMS_STATE_OR_PROVINCE);
        stateOrProvinceElement.appendChild(address.getCity());
        contactAddressElement.appendChild(stateOrProvinceElement);

        // PostCode
        Element postCodeElement = new Element(XMLSymbols.LABEL_WMS_POST_CODE);
        postCodeElement.appendChild(address.getPostalCode());
        contactAddressElement.appendChild(postCodeElement);

        // Country
        Element countryElement = new Element(XMLSymbols.LABEL_WMS_COUNTRY);
        countryElement.appendChild(address.getCountry());
        contactAddressElement.appendChild(countryElement);

        Phone phone = serviceProvider.getServiceContact().getContactInfo().getPhone();
        // ContactVoiceTelephone
        Element contactVoiceTelephoneElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_VOICE_TELEPHONE);
        String voicePhone = phone.getVoicePhones().get(0);
        contactVoiceTelephoneElement.appendChild(voicePhone);
        contactInformationElement.appendChild(contactVoiceTelephoneElement);

        // ContactFacsimileTelephone (optional)
        if (!phone.getFacsimilePhones().isEmpty()) {
            Element contactFacsimileTelephoneElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_FACSIMILE_TELEPHONE);
            String facsimilePhone = phone.getFacsimilePhones().get(0);
            contactFacsimileTelephoneElement.appendChild(facsimilePhone);
            contactInformationElement.appendChild(contactFacsimileTelephoneElement);
        }

        // ContactElectronicMailAddress
        Element contactEmailElement = new Element(XMLSymbols.LABEL_WMS_CONTACT_EMAIL);
        String email = address.getElectronicMailAddresses().get(0);
        contactEmailElement.appendChild(email);
        contactInformationElement.appendChild(contactEmailElement);

        return contactInformationElement;
    }

    /**
     * Build the Capability element to replace from the template file of
     * WMS_Capabilities element
     *
     * @return
     */
    private String buildCapability() throws WMSLayerNotExistException, PetascopeException {

        String capability = Templates.getTemplate(Templates.WMS_GET_CAPABILITIES_CAPABILITY);
        // Replace petascope url for 2 DCPTypes inside the GetCapabilities element
        capability = capability.replaceAll(Templates.PETASCOPE_URL, ConfigManager.PETASCOPE_ENDPOINT_URL);

        // GetMap
        Element getMapElement = new Element(XMLSymbols.LABEL_WMS_GET_MAP);
        // Format
        for (String format : supportedFormats) {
            Element formatElement = new Element(XMLSymbols.LABEL_WMS_FORMAT);
            formatElement.appendChild(format);

            getMapElement.appendChild(formatElement);
        }

        // DCPType
        // NOTE: The URL in DCPType element is important as WMS client (e.g: QGIS) will redirect to this URL
        // So if it is set to e.g: http://localhost:8080 but testing is http://localhost:8081 then the reques from client will send to http://localhost:8080 (!)
        String dcpType = Templates.getTemplate(Templates.WMS_GET_CAPABILITIES_CAPABILITY_DCPTYPE);
        dcpType = dcpType.replaceAll(Templates.PETASCOPE_URL, ConfigManager.PETASCOPE_ENDPOINT_URL);
        getMapElement.appendChild(dcpType);

        // Replace the get map replacement text in template
        capability = capability.replace(Templates.WMS_GET_CAPABILITIES_CAPABILITY_GET_MAP_ELEMENT, getMapElement.toXML());

        // Exception element
        Element exceptionElement = new Element(XMLSymbols.LABEL_WMS_EXCEPTION);
        for (String exceptionFormat : supportedExceptions) {
            Element formatElement = new Element(XMLSymbols.LABEL_WMS_FORMAT);
            formatElement.appendChild(exceptionFormat);

            exceptionElement.appendChild(formatElement);
        }

        // Replace the exception element replacement text in template
        capability = capability.replace(Templates.WMS_GET_CAPABILITIES_CAPABILITY_EXCEPTION_ELEMENT, exceptionElement.toXML());

        // Replace the layer replacement text in template
        String layers = this.buildLayers();
        capability = capability.replace(Templates.WMS_GET_CAPABILITIES_CAPABILITY_LAYER_ELEMENTS, layers);

        return capability;
    }

    /**
     * Build the list of layers element which is child element of an outer Layer
     * element
     *
     * @return
     */
    private String buildLayers() throws WMSLayerNotExistException, PetascopeException {
        // All WMS layers
        List<Layer> layers = this.wmsRepostioryService.readAllLayersFromCaches();
        
        // to build layerElements
        List<Element> layerElements = new ArrayList<>();
        for (Layer layer : layers) {
            Element layerElement = this.buildLayerElement(layer);
            
            if (layerElement != null) {
                layerElements.add(layerElement);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Element layerElement : layerElements) {
            sb.append(layerElement.toXML());
        }

        return sb.toString();
    }

    /**
     * Build a layer element
     *
     * @param layer
     * @return
     */
    private Element buildLayerElement(Layer layer) throws PetascopeException {
        Element layerElement = new Element(XMLSymbols.LABEL_WMS_LAYER);

        // All the attributes for one layer element
        LayerAttribute layerAttribute = layer.getLayerAttribute();
        Attribute queryableAttribute = new Attribute(LayerAttribute.QUERYABLE, String.valueOf(layerAttribute.getQueryable()));
        Attribute cascadedAttribute = new Attribute(LayerAttribute.CASCADED, String.valueOf(layerAttribute.getCascaded()));
        Attribute opaqueAttribute = new Attribute(LayerAttribute.OPAQUE, String.valueOf(layerAttribute.getOpaque()));
        Attribute noSubsetsAttribute = new Attribute(LayerAttribute.NO_SUBSETS, String.valueOf(layerAttribute.getNoSubsets()));
        Attribute fixedWidthAttribute = new Attribute(LayerAttribute.FIXED_WIDTH, String.valueOf(layerAttribute.getFixedWidth()));
        Attribute fixedHeightAttribute = new Attribute(LayerAttribute.FIXED_HEIGHT, String.valueOf(layerAttribute.getFixedHeight()));
        layerElement.addAttribute(queryableAttribute);
        layerElement.addAttribute(cascadedAttribute);
        layerElement.addAttribute(opaqueAttribute);
        layerElement.addAttribute(noSubsetsAttribute);
        layerElement.addAttribute(fixedWidthAttribute);
        layerElement.addAttribute(fixedHeightAttribute);

        // Name
        Element nameElement = new Element(XMLSymbols.LABEL_WMS_NAME);

        String layerName = layer.getName(); 
        nameElement.appendChild(layerName);
        layerElement.appendChild(nameElement);

        // Title        
        Element titleElement = new Element(XMLSymbols.LABEL_WMS_TITLE);
        titleElement.appendChild(layer.getTitle());
        layerElement.appendChild(titleElement);

        // Abstract
        Element abstractElement = new Element(XMLSymbols.LABEL_WMS_ABSTRACT);
        String layerAbstract = layer.getLayerAbstract();        
        abstractElement.appendChild(layerAbstract);
        layerElement.appendChild(abstractElement);
        
        Coverage coverage = this.coverageRepositoryService.readCoverageFromLocalCache(layer.getName());
        try {
            coverage = this.coverageRepositoryService.readCoverageBasicMetadataByIdFromCache(layer.getName());
            // The WMS capabilities schema does not allow a customized metadata element here or anywhere else
            // if (coverage != null) {
            //     Element customizedMetadataElement = this.wcsGMLGetCapabilitiesBuild.createCustomizedCoverageMetadataElement(coverage);
            //     if (customizedMetadataElement != null) {
            //         layerElement.appendChild(customizedMetadataElement);
            //     }
            // }
        } catch (PetascopeException ex) {
            if (ex.getExceptionCode().equals(ExceptionCode.NoSuchCoverage)) {
                // The associated coverage with the layer was changed, log an warning instead of throwing exception
                log.warn("Coverage associated with the layer: " + layer.getName() + " doees not exist.");
                return null;
            }
        }

        // KeywordList
        if (layer.getKeywordList().size() > 0) {
            Element keywordListElement = this.buildKeywordListElement(layer.getKeywordList());
            layerElement.appendChild(keywordListElement);
        }

        // CRSs (Current only contain one native CRS of coverage for geo XY axes)
        Element crsElement = new Element(XMLSymbols.LABEL_WMS_CRS);
        crsElement.appendChild(layer.getCrss().get(0));
        layerElement.appendChild(crsElement);

        // EX_GeographicBoundingBox 
        Wgs84BoundingBox wgs84BBox = coverage.getEnvelope().getEnvelopeByAxis().getWgs84BBox();
        
        EXGeographicBoundingBox exGeographicBoundingBox = new EXGeographicBoundingBox(wgs84BBox);
        layer.setExGeographicBoundingBox(exGeographicBoundingBox);
        String exBBoxRepresentation = layer.getExGeographicBoundingBox().getReprenstation();
        layerElement.appendChild(exBBoxRepresentation);

        // BoundingBox (Current only contain one bounding box for geo XY axes)
        // NOTE: WMS 1.3 use the order from CRS (e.g: CRS:4326, order is Lat, Long, CRS:3857, order is E, N)
        // @TODO: If layer has multiple bboxes (!)
        String bboxRepresentation = layer.getBoundingBoxes().get(0).getRepresentation();
        layerElement.appendChild(bboxRepresentation);

        // Dimension (Current not support yet)
        if (layer.getDimensions().size() > 0) {
            StringBuilder dimensions = new StringBuilder();
            for (Dimension dimension : layer.getDimensions()) {
                dimensions.append(dimension.getRepresentation());
            }

            layerElement.appendChild(dimensions.toString());
        }

        // Each layer contains zero or multiple styles (by default, style is as same as layer).
        if (!layer.getStyles().isEmpty()) {
            String styles = this.buildStyles(layer.getName(), layer.getStyles());
            layerElement.appendChild(styles);
        }

        return layerElement;
    }

    /**
     * Build a list of Style elements for a layer
     *
     * @param styles
     * @return
     */
    private String buildStyles(String layerName, List<Style> styles) throws PetascopeException {
        StringBuilder result = new StringBuilder();
        for (Style style : styles) {
            String styleRepresentation = "";
            try {
                styleRepresentation = this.getRepresentation(style);
            } catch (Exception ex) {
                throw new PetascopeException(ExceptionCode.InternalComponentError, 
                                            "Cannot create representation for style '" + style.getName() + "' of layer '" + layerName + "'"
                                            + ". Reason: " + ex.getMessage(), ex);
            }
            
            result.append(styleRepresentation);
        }

        return result.toString();
    }
    
    /**
     * Return the Style XML element representation in string
     *
     * @return
     */
    public String getRepresentation(Style style) throws PetascopeException {
        Element styleElement = new Element(XMLSymbols.LABEL_WMS_STYLE);

        // Name
        Element nameElement = new Element(XMLSymbols.LABEL_WMS_NAME);
        nameElement.appendChild(style.getName());
        styleElement.appendChild(nameElement);

        // Title
        Element titleElement = new Element(XMLSymbols.LABEL_WMS_TITLE);
        titleElement.appendChild(style.getTitle());
        styleElement.appendChild(titleElement);

        // Abstract
        Element abstractElement = this.buildStyleAbstractElemt(style);
        styleElement.appendChild(abstractElement);

        if (style.getLegendURL() != null) {
            styleElement.appendChild(style.getLegendURL().getRepresentation());
        }

        return styleElement.toXML();
    }
    
    /**
     *
     * Build XML element for a style's abstract.
     */
    private Element buildStyleAbstractElemt(Style style) throws PetascopeException {
        Element abstractElement = new Element(XMLSymbols.LABEL_WMS_ABSTRACT);
        
        // User's abstract for the style
        String styleAbstractStr = style.getStyleAbstract();
        
        // Rasdaman's abstract for the style (non-standard)
        String rasqlQueryFragment = style.getRasqlQueryFragment();
        String wcpsQueryFragment = style.getWcpsQueryFragment();
        Byte colorTableTypeCode = style.getColorTableType();
        String colorTableType = null;
        String colorTableDefinition = null;
        
        if (colorTableTypeCode != null) {
            colorTableType = ColorTableType.getType(colorTableTypeCode);
            colorTableDefinition = style.getColorTableDefinition();
        }
        
        Element rasdamanElement = new Element(XMLSymbols.LABEL_RASDAMAN);
        
        if (!StringUtils.isEmpty(rasqlQueryFragment)) {
            Element rasqlQueryFragmentElement = new Element(XMLSymbols.LABEL_WMS_RASQL_QUERY_FRAGMENT);
            rasqlQueryFragmentElement.appendChild(rasqlQueryFragment);
            rasdamanElement.appendChild(rasqlQueryFragmentElement);
        }
        
        if (!StringUtils.isEmpty(wcpsQueryFragment)) {
            Element wcpsQueryFragmentElement = new Element(XMLSymbols.LABEL_WMS_WCPS_QUERY_FRAGMENT);
            wcpsQueryFragmentElement.appendChild(wcpsQueryFragment);
            rasdamanElement.appendChild(wcpsQueryFragmentElement);
        }
        
        Element colorTableElement = new Element(XMLSymbols.LABEL_WMS_COLOR_TABLE);
        
        if (!StringUtils.isEmpty(colorTableType)) {
            Element colorTableTypeElement = new Element(XMLSymbols.LABEL_WMS_COLOR_TABLE_TYPE);
            colorTableTypeElement.appendChild(colorTableType);
            colorTableElement.appendChild(colorTableTypeElement);
        
            Element colorTableDefinitionElement = new Element(XMLSymbols.LABEL_WMS_COLOR_TABLE_DEFINITION);
            
            if (colorTableType.toLowerCase().equals(ColorTableType.SLD.toString().toLowerCase())) {
                // NOTE: SLD is another nested XML element
                colorTableDefinitionElement.appendChild(XMLUtil.parseXmlFragment(colorTableDefinition));
            } else {
                colorTableDefinitionElement.appendChild(colorTableDefinition);
            }
            
            colorTableElement.appendChild(colorTableDefinitionElement);
        }
        
        if (colorTableElement.getChildCount() > 0) {
            rasdamanElement.appendChild(colorTableElement);
        }
        
        String content = styleAbstractStr + "\n" + XMLUtil.enquoteCDATA(rasdamanElement.toXML());
        abstractElement.appendChild(content);
        
        return abstractElement;
    }
    

    /**
     * Build a KeywordList element from list of keywords
     *
     * @param keywords
     * @return KeywordList element
     */
    private Element buildKeywordListElement(List<String> keywords) {
        Element keywordsElement = new Element(XMLSymbols.LABEL_KEYWORDLIST);
        // KeywordList element can contain multiple Keyword elements
        for (String keyword : keywords) {
            Element keywordElement = new Element(XMLSymbols.LABEL_KEYWORD);
            keywordElement.appendChild(keyword);

            keywordsElement.appendChild(keywordElement);
        }

        return keywordsElement;
    }
}
