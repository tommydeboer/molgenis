package org.molgenis.api.permissions.exceptions;

import org.molgenis.util.exception.CodedRuntimeException;

public class UnsupportedPermissionQueryException extends CodedRuntimeException {
  private static final String ERROR_CODE = "PRM04";

  public UnsupportedPermissionQueryException() {
    super(ERROR_CODE);
  }

  @Override
  protected Object[] getLocalizedMessageArguments() {
    return new Object[0];
  }
}
