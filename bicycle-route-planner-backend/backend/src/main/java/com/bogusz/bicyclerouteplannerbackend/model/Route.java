package com.bogusz.bicyclerouteplannerbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Size(max = 150)
    @NotNull
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 50)
    @Column(name = "route_type", length = 50)
    private String routeType;

    @Column(name = "target_distance")
    private Double targetDistance;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "elevation_gain")
    private Double elevationGain;

    @Column(name = "elevation_loss")
    private Double elevationLoss;

    @Column(name = "start_point", columnDefinition = "geometry(Point, 4326)")
    private Point startPoint;

    @Column(name = "route_geom", nullable = false, columnDefinition = "geometry(LineStringZ, 4326)")
    private LineString routeGeom;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}