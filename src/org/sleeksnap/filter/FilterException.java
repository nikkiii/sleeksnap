package org.sleeksnap.filter;

/**
 * An Exception which can be thrown when a Filter experiences an error that
 * should stop the upload
 * 
 * @author Nikki
 * 
 */
@SuppressWarnings("serial")
public class FilterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -71541266967646112L;
	/**
	 * The separate error message, this is NOT the same as e.getMessage()
	 */
	private String errorMessage;

	public FilterException(final Exception e) {
		super(e);
	}

	public FilterException(final Exception e, final String errorMessage) {
		super(e);
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
