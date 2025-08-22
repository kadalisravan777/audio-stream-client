package com.audiostream.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenedMessage {
	private String version;
	private String id;
	private String type;
	private Integer seq;
	private Integer clientseq;
	private OpenedParameters parameters;
}
