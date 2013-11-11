package com.spotify.helios.common.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SetGoalResponse {
  public enum Status {
    OK,
    JOB_NOT_FOUND,
    AGENT_NOT_FOUND,
    JOB_NOT_DEPLOYED,
    ID_MISMATCH
  }

  private final Status status;
  private final String job;
  private final String agent;

  public SetGoalResponse(@JsonProperty("status") Status status,
                         @JsonProperty("agent") String agent,
                         @JsonProperty("job") String job) {
    this.status = status;
    this.job = job;
    this.agent = agent;
  }

  public Status getStatus() {
    return status;
  }

  public String getJob() {
    return job;
  }

  public String getAgent() {
    return agent;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("JobDeployResponse")
        .add("status", status)
        .add("agent", agent)
        .add("job", job)
        .toString();
  }
}
