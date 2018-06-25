package simplenem12;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * This class implements {@link SimpleNem12Parser} and has concrete implementation of the parsing of NEM12 files
 * 
 * @author Rohan Kumar Jha
 *
 */
public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	private static final String RECORD_DELIMITTER = ",";

	private ParserCurrentParams parserCurrentParams = new ParserCurrentParams();
	
	/**
	 * Reads the input file line by line and returns the collection of the parsed model of NEM12 files
	 * 
	 * @param simpleNem12File Input file to be parsed
	 * @return Collection of MeterRead
	 */
	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File){
		try (Stream<String> lines = Files.lines(Paths.get(simpleNem12File.getAbsolutePath()));){
			lines.forEach(this::processLineSimpleNem12);
			return parserCurrentParams.meterReads;
		} catch (IOException ioe) {
			System.err.println("IOException occured during reading the file...");
			//throw ioe; //This exception should be thrown ideally but dont want to change the existing interface
		}
		return null;
	}
	
	/**
	 * Validates the record types and there end positions in the file, throws IllegalArgumentException on failed validation
	 * 
	 * @param lineParams : Split parameters of the current line segment
	 * @throws IllegalArgumentException
	 * 
	 */
	private void validateRecordTypes(String[] lineParams) throws IllegalArgumentException {

		if (Arrays.stream(RecordType.values()).filter(enumVal -> enumVal.getRecordTypeValue().equals(lineParams[0])).count() == 0) {
			throw new IllegalArgumentException("Invalid Record type exists!!!");
		}

		if (RecordType.FILE_START.getRecordTypeValue().equals(lineParams[0])) {
			if (lineParams.length != 1 || parserCurrentParams.currentIndex != 0)
				throw new IllegalArgumentException("Invalid file start!!!");
		} else if (RecordType.FILE_END.getRecordTypeValue().equals(lineParams[0])) {
			if (lineParams.length != 1 || parserCurrentParams.fileClosedRecordTypeExists)
				throw new IllegalArgumentException("Invalid file end!!!");
			parserCurrentParams.fileClosedRecordTypeExists = true;
		}
	}

	/**
	 * Validates the record types
	 * Validates meter read block and meter volume line segments
	 * Creates the parsed model representing every input line segments
	 * 
	 * @param line : Input line from the stream
	 * @throws IllegalArgumentException
	 *
	 */
	private void processLineSimpleNem12(String line) throws IllegalArgumentException {
		if (line == null || "".equals(line.trim()))
			return;

		String[] lineParams = line.split(RECORD_DELIMITTER);

		validateRecordTypes(lineParams);

		if (lineParams[0].equals(RecordType.METER_READ_BLOCK.getRecordTypeValue())) { 
			if(lineParams.length != 3) throw new IllegalArgumentException("Invalid meter block exists!!!"); 
			if (!EnergyUnit.KWH.name().equals(lineParams[2]))
				throw new IllegalArgumentException("Invalid meter read block energy unit!!!");
			MeterRead meterRead = new MeterRead(lineParams[1], EnergyUnit.KWH);
			parserCurrentParams.currentMeterRead = meterRead;
			parserCurrentParams.meterReads.add(meterRead);
		} 

		if (lineParams[0].equals(RecordType.METER_READ_VOLUME.getRecordTypeValue())) {
			if(lineParams.length != 4) throw new IllegalArgumentException("Invalid meter volume exists!!!");
			if(null == parserCurrentParams.currentMeterRead) throw new IllegalArgumentException("Meter block doesn't exist for the meter volume!!!");
			if (!Quality.A.name().equals(lineParams[3]) && !Quality.E.name().equals(lineParams[3]))
				throw new IllegalArgumentException("Invalid meter read volume quality!!!");
			LocalDate localDate = LocalDate.parse(lineParams[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
			parserCurrentParams.currentMeterRead.appendVolume(localDate,
					new MeterVolume(new BigDecimal(lineParams[2]), Quality.valueOf(lineParams[3])));
		} 
		parserCurrentParams.currentIndex++;
	}

	/**
	 * Enums to define valid record types in NEM12 files and their values
	 * 
	 * @author Rohan Kumar Jha
	 */
	private enum RecordType {
		FILE_START("100"), FILE_END("900"), METER_READ_BLOCK("200"), METER_READ_VOLUME("300");

		private String recordTypeValue;

		private RecordType(String recordTypeValue) {
			this.recordTypeValue = recordTypeValue;
		}

		public String getRecordTypeValue() {
			return this.recordTypeValue;
		}
	}
	
	/**
	 * Private inner class to hold the current values of the parser pointer
	 * 
	 * @author Rohan Kumar Jha
	 */
	private class ParserCurrentParams{
		private Collection<MeterRead> meterReads = new HashSet<>();
		private MeterRead currentMeterRead;
		private long currentIndex = 0;
		private boolean fileClosedRecordTypeExists=false;
	}
}
