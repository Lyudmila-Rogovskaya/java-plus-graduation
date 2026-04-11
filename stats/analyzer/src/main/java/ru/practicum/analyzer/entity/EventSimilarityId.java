package ru.practicum.analyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarityId implements Serializable {

    private Long eventA;
    private Long eventB;

}
