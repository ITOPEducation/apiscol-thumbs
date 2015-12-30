package fr.ac_versailles.crdp.apiscol.thumbs.representations;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;

public interface IEntitiesRepresentationBuilder<T> {

	T getThumbsInformationForMetadata(URI baseUri, String metadataId,
			String status);

	T getVoidSuggestion();

	T getThumbsRepresentation(URI baseUri,
			List<String> metadataList,
			String status);

	T getThumbsListRepresentation(Set<String> set,
			String metadataId, String etag,
			URI baseUri);

	MediaType getMediaType();
}
