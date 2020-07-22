package com.appdirect.tika.api.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import io.swagger.annotations.ApiParam;
import reactor.core.publisher.Mono;

@RestController
public class TikaApi {

	@RequestMapping(value = "api/tika/extractcontent/",
		method = RequestMethod.GET)
	Mono<ResponseEntity<String>> estimateTaxes(@ApiParam(value = "path", required = true) String path) throws IOException, TikaException, SAXException {
		System.out.println("path: "+path);
		File initialFile = new File(path);
		InputStream stream = new FileInputStream(initialFile);
		Parser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		ContentHandler handler = new BodyContentHandler(-1);
		ParseContext context = new ParseContext();
		parser.parse(stream, handler, metadata, context);
		stream.close();
		FileWriter writer = new FileWriter(path + ".out");
		String out = handler.toString();
		System.out.println(out);
		writer.write(out);
		writer.close();
		return Mono.just(ResponseEntity.ok(path+".out"));
	}

}

